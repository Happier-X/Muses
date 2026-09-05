package com.muses.player.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.PlaylistRepository
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.core.model.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 歌单详情 ViewModel（安卓侧）：数据核 [PlaylistDetailCoreViewModel]（commonMain）
 * + 播放连接扩展（currentSongId 高亮 / 单曲/整体入队经 PlayerConnection）。
 */
class PlaylistDetailViewModel constructor(
    repository: PlaylistRepository,
    private val playerConnection: PlayerConnection,
) : PlaylistDetailCoreViewModel(repository) {

    /** 当前播放中的歌曲 id（行高亮用，row--playing） */
    val currentSongId: StateFlow<String?> = playerConnection.currentMediaItem
        .map { it?.mediaId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 单曲点击：以整个歌单为队列从该曲开始播（Web onPlaySong → playSong 内部以列表入队） */
    fun playSongFromList(songId: String) {
        val id = playlistId.value ?: return
        viewModelScope.launch {
            val songs = repository.getSongs(id)
            if (songs.any { it.id == songId }) {
                playerConnection.play(songId, songs)
            }
        }
    }

    /** 整体入队播放：从首曲开始按当前顺序播放 */
    fun playAll() {
        val id = playlistId.value ?: return
        viewModelScope.launch {
            val songs: List<Song> = repository.getSongs(id)
            if (songs.isNotEmpty()) playerConnection.play(songs.first().id, songs)
        }
    }
}

/** 播放列表 ViewModel 装配（P2a Hilt→Koin；安卓侧含播放依赖与 PlaylistsViewModel）。 */
val playlistModule = module {
    viewModel { PlaylistsViewModel(get()) }
    viewModel { PlaylistDetailViewModel(get(), get()) }
}
