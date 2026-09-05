package com.muses.player.feature.sources

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 音源页 ViewModel 装配·安卓侧（U11 KMP：扫描器/播放连接经构造注入真实现）。 */
val sourcesModule = module {
    viewModel {
        SourcesViewModel(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
        )
    }
}
