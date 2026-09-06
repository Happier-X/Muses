package com.muses.player.core.media.scanner

import com.muses.player.core.data.db.SongTags
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.Song

/**
 * 播放时懒扫描编排（U26 上收自安卓 PlaybackService.onEvents，android/desktop 双端共用）。
 *
 * 契约（与安卓侧逐字对齐）：
 * - 仅对 tagsVersion < TAGS_VERSION（文件名建库占位 0）执行；
 * - 已刮削字段（metaSources 非空标记）不得被文件旧标签覆盖（重刮削后播放旧值回归根因）；
 * - 歌词有 scrape/embedded 标记（lyricsSource 非空且 lyrics 非空）同样跳过；
 * - 无实际更新仍抬升 tagsVersion，避免下次重复探测；
 * - 入库走 SongRepository.upsert 唯一路径（同步重建派生索引）。
 *
 * @param tags 文件标签快照（调用方负责读取：安卓经 AudioTagReader Range 探测，
 *   桌面经 JaudiotaggerTagPort 读本地缓存文件）；null = 读取失败，本次跳过（下次重试）。
 * @return 融合后的 Song（需入库），或 null（无需处理：版本已齐 / 标签读取失败）。
 */
object PlaybackLazyScan {

    /** 文件标签快照（平台无关输入；两端各自读取后喂入） */
    data class FileTags(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        val coverUri: String? = null,
        val durationMs: Long = 0L,
    )

    fun merge(song: Song, tags: FileTags?): Song? {
        if (song.tagsVersion >= SongTags.TAGS_VERSION) return null
        if (tags == null) return null

        val entity = song
        val ms = song.metaSources
        // 已刮削字段跳过覆盖，未标记字段才允许用文件标签补齐
        val resolvedTitle =
            if (ms?.title != null) song.title else tags.title?.takeIf { it.isNotBlank() } ?: song.title
        val resolvedArtist = if (ms?.artist != null) song.artist else tags.artist ?: song.artist
        val resolvedAlbum = if (ms?.album != null) song.album else tags.album ?: song.album
        val resolvedCover = if (ms?.cover != null) song.coverUri else tags.coverUri ?: song.coverUri
        // 歌词：有 scrape/embedded 标记时同样跳过（lyricsSource 非空视为已刮削）
        val resolvedLyrics =
            if (song.lyricsSource != null && !song.lyrics.isNullOrBlank()) song.lyrics else tags.lyrics ?: song.lyrics
        val hasUpdate = resolvedTitle != entity.title ||
            resolvedArtist != entity.artist ||
            resolvedAlbum != entity.album ||
            resolvedCover != entity.coverUri ||
            resolvedLyrics != entity.lyrics ||
            tags.durationMs > entity.durationMs
        return if (hasUpdate) {
            song.copy(
                title = resolvedTitle,
                artist = resolvedArtist,
                album = resolvedAlbum,
                lyrics = resolvedLyrics,
                coverUri = resolvedCover,
                durationMs = tags.durationMs.coerceAtLeast(entity.durationMs),
                durationSec = (tags.durationMs / 1000).coerceAtLeast(entity.durationSec),
                tagsVersion = SongTags.TAGS_VERSION,
            )
        } else {
            // 无实际更新：仍抬升 tagsVersion 以避免重复探测
            // （守卫已保证不覆盖刮削值；无标签文件亦按原契约抬升，下次显示文件名不重复探测）
            song.copy(tagsVersion = SongTags.TAGS_VERSION)
        }
    }
}

/** SongEntity 便捷入口（安卓 PlaybackService 侧用；桌面直接用 domain 版） */
fun com.muses.player.core.data.db.SongEntity.mergeLazyTags(tags: PlaybackLazyScan.FileTags?): Song? =
    PlaybackLazyScan.merge(toDomain(), tags)
