package com.muses.player.feature.playlist

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 播放列表 ViewModel 装配（U19 全量上收：页面/VM 全在 commonMain，播放依赖经
 * PlaybackPort 注入，双端同一模块；原 androidMain playlistModule 并入后废弃）。
 */
val playlistCoreModule = module {
    viewModel { AddToPlaylistViewModel(get()) }
    viewModel { PlaylistsViewModel(get()) }
    viewModel { PlaylistDetailViewModel(get(), get()) }
}
