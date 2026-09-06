package com.muses.player.feature.sources

import com.muses.player.core.playback.PlaybackPort
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 音源页 ViewModel 装配（U20 全量上收：WebDAV 浏览/表单 + 音源页主 VM，双端同模块）。
 * 播放队列清理回调取 PlaybackPort 绑定（安卓=PlayerConnection，桌面=DesktopPlayerHook）。
 */
val sourcesCoreModule = module {
    viewModel { WebDavBrowseViewModel(get()) }
    viewModel { WebDavFormViewModel(get(), get(), get()) }
    viewModel {
        val port: PlaybackPort = get()
        SourcesViewModel(
            get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(),
            get(),
            onRemoveFromQueue = port::removeFromQueue,
        )
    }
}
