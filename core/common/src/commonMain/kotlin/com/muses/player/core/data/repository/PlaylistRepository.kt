package com.muses.player.core.data.repository

import com.muses.player.core.model.Playlist
import com.muses.player.core.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

/**
 * 播放列表仓库（U10 从 :core:data 上收：接口进 commonMain，实现进 jvmShared，
 * 安卓侧同包名零改动）。
 * 注意：入队播放（playQueue）依赖 M1 的 PlayerConnection，本阶段仅暴露 [observePlaylistSongIds]，
 * 接线留 TODO 给阶段 1 之后。
 */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    /** 各歌单有效歌曲数（playlistId → count；曲库删除实时联动） */
    fun observeValidCounts(): Flow<Map<String, Int>>

    fun observePlaylist(id: String): Flow<PlaylistWithSongs?>

    /** 入队播放接口预留：按播放顺序返回 songIds */
    fun observePlaylistSongIds(id: String): Flow<List<String>>

    /** 按播放顺序返回歌曲领域模型（供整体入队播放） */
    suspend fun getSongs(id: String): List<com.muses.player.core.model.Song>

    suspend fun createPlaylist(name: String): String

    suspend fun renamePlaylist(id: String, name: String)

    suspend fun deletePlaylist(id: String)

    /** 追加歌曲；已在列表内的 songId 自动跳过（去重） */
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>)

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    suspend fun moveSong(playlistId: String, fromPosition: Int, toPosition: Int)
}
