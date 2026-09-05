package com.muses.player.feature.playlist

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 播放列表 ViewModel 装配·共享核（U10 KMP：纯 repository 依赖，双端可消费）。 */
val playlistCoreModule = module {
    viewModel { AddToPlaylistViewModel(get()) }
}
