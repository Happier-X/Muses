package com.muses.player.feature.sources

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 音源页 ViewModel 装配·共享核（U11 KMP：WebDAV 浏览/表单 ViewModel，双端可消费）。 */
val sourcesCoreModule = module {
    viewModel { WebDavBrowseViewModel(get()) }
    viewModel { WebDavFormViewModel(get(), get(), get()) }
}
