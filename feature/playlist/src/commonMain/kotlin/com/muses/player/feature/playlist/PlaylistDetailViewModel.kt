package com.muses.player.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.PlaylistRepository
import com.muses.player.core.model.Song
import com.muses.player.core.playback.PlaybackPort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单详情 ViewModel（U19 全量上收 commonMain）：数据核 [PlaylistDetailCoreViewModel]
 * + 播放扩展（currentSongId 高亮 / 单曲/整体入队）。播放连接经 [PlaybackPort] 端口注入，
 * 不再依赖 core:media（与 feature:library U9 同形态）。
 */
class PlaylistDetailViewModel constructor(
    repository: PlaylistRepository,
    private val playback: PlaybackPort,
) : PlaylistDetailCoreViewModel(repository) {

    /** 当前播放中的歌曲 id（行高亮用，row--playing） */
    val currentSongId: StateFlow<String?> = playback.currentSongId

    /** 单曲点击：以整个歌单为队列从该曲开始播（Web onPlaySong → playSong 内部以列表入队） */
    fun playSongFromList(songId: String) {
        val id = playlistId.value ?: return
        viewModelScope.launch {
            val songs = repository.getSongs(id)
            if (songs.any { it.id == songId }) {
                playback.play(songId, songs)
            }
        }
    }

    /** 整体入队播放：从首曲开始按当前顺序播放 */
    fun playAll() {
        val id = playlistId.value ?: return
        viewModelScope.launch {
            val songs: List<Song> = repository.getSongs(id)
            if (songs.isNotEmpty()) playback.play(songs.first().id, songs)
        }
    }
}
