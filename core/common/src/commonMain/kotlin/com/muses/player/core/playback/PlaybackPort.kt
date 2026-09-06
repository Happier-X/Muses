package com.muses.player.core.playback

import com.muses.player.core.model.Song
import com.muses.player.core.model.playback.RepeatMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放状态整型常量（U12）：与 Media3 `Player.STATE_*` / `Player.REPEAT_MODE_*`
 * 数值冻结一致（桌面 JvmPlaybackStates 同值，P3-S2 已对齐）。
 * commonMain 不能 import Media3，经此常量对象解耦。
 */
object PlaybackStates {
    const val STATE_IDLE = 1
    const val STATE_BUFFERING = 2
    const val STATE_READY = 3
    const val STATE_ENDED = 4

    const val REPEAT_MODE_OFF = 0
    const val REPEAT_MODE_ONE = 1
    const val REPEAT_MODE_ALL = 2
}

/**
 * 播放端口·UI 全量面（U12）：在 P1 冻结的驱动端口 [PlayerPort] 之上补齐读侧观测
 * 与队列/换曲控制。commonMain 的 PlayerViewModel/QueueViewModel 经此驱动双端播放栈
 * （安卓 PlayerConnection/Media3；桌面 JvmPlayerPort/VLCJ 播放页统一时接入）。
 *
 * 队列展示字段（标题/艺术家）不进端口——端口只暴露 songId 有序集，
 * 由 VM 按曲库组合（双端同源 Room，见 QueueRow 组合链）。
 */
interface PlaybackPort : PlayerPort {
    // ── 读侧 ──
    val isPlaying: StateFlow<Boolean>

    /** 当前曲 id（null = 无当前曲；安卓取 MediaItem.mediaId，桌面 hook.currentSongId） */
    val currentSongId: StateFlow<String?>

    /** 兜底封面（播放器实时 metadata 的 artworkUri；桌面暂空流） */
    val artworkUri: StateFlow<String?>

    val duration: StateFlow<Long>

    /** 当前队列（仅 songId 有序集） */
    val queueSongIds: StateFlow<List<String>>

    // ── 写侧 ──

    /** 从歌曲列表中选择 songId 开始播放（入队语义，见安卓 PlayerConnection.play） */
    fun play(songId: String, songs: List<Song>)

    /** 暂停/继续（语义同安卓 PlayerConnection.playPause） */
    fun playPause()

    fun playAtIndex(index: Int)

    fun removeQueueItemAt(index: Int)

    fun clearQueueItems()

    fun skipToNext()

    fun skipToPrevious()

    /** 即席位置查询（UI 轮询用；ExoPlayer/媒体控制器同步读） */
    fun currentPosition(): Long

    fun clearPlaybackError()

    /** 重置失败恢复链（重试播放前调用） */
    fun resetRecovery()
}
