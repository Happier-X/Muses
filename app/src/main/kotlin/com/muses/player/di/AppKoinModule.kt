package com.muses.player.di

import com.muses.player.core.data.di.databaseModule
import com.muses.player.core.data.repository.repositoryModule
import com.muses.player.core.data.tag.tagModule
import com.muses.player.core.lyrics.di.lyricsModule
import com.muses.player.core.media.playback.playbackModule
import com.muses.player.core.scrape.di.scrapeModule
// U11：webdavCoreModule（Ktor 客户端/AuthRegistry/限流器跨平台绑定）+ webdavModule（OkHttp 流播绑定）
import com.muses.player.core.webdav.webdavCoreModule
import com.muses.player.core.webdav.webdavModule
import com.muses.player.feature.library.libraryModule
import com.muses.player.feature.player.playerModule
// U10：playlistModule（安卓侧 VM）+ playlistCoreModule（commonMain 共享 VM）双装配
import com.muses.player.feature.playlist.playlistCoreModule
import com.muses.player.feature.playlist.playlistModule
import com.muses.player.feature.scrape.scrapeFeatureModule
// U11：sourcesModule（安卓扫描 VM）+ sourcesCoreModule（WebDAV 浏览/表单共享 VM）双装配
import com.muses.player.feature.sources.sourcesCoreModule
import com.muses.player.feature.sources.sourcesModule
import com.muses.player.navigation.MainViewModel
import com.muses.player.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** app 层 ViewModel 装配（P2a Hilt→Koin：MainViewModel + SettingsViewModel）。 */
val appModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}

/** 全量模块聚合：`MusesApplication.startKoin` 唯一入口（P2a design §3）。 */
val appModules = listOf(
    databaseModule,
    tagModule,
    repositoryModule,
    lyricsModule,
    playbackModule,
    scrapeModule,
    webdavModule,
    webdavCoreModule,
    libraryModule,
    playerModule,
    playlistModule,
    playlistCoreModule,
    scrapeFeatureModule,
    sourcesModule,
    sourcesCoreModule,
    appModule,
)
