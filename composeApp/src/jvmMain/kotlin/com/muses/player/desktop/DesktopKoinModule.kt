package com.muses.player.desktop

import com.muses.player.core.data.log.RingBufferErrorLogStore
import com.muses.player.core.data.repository.AlbumRepository
import com.muses.player.core.data.repository.ArtistRepository
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.RoomAlbumRepository
import com.muses.player.core.data.repository.RoomArtistRepository
import com.muses.player.core.data.repository.RoomSongRepository
import com.muses.player.core.data.repository.RoomSourceRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.desktop.di.DesktopCredentials
import com.muses.player.core.webdav.webdavCoreModule
import com.muses.player.feature.library.libraryModule
import com.muses.player.feature.player.playerModule
import com.muses.player.feature.sources.sourcesCoreModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * 桌面运行时单例（U12）：播放 hook 唯一实例——Main 壳/托盘/SMTC/Koin 端口绑定共用，
 * 维持「Screens 与托盘共享同一播放状态源」契约（hook 依赖 composeApp 层，不能落 :desktop）。
 */
object DesktopRuntime {

    @Volatile private var playerHook: DesktopPlayerHook? = null

    fun playerHook(): DesktopPlayerHook =
        playerHook ?: synchronized(this) {
            playerHook ?: DesktopPlayerHook().also { playerHook = it }
        }
}

/**
 * U9 桌面装配：共享 ViewModel（:feature:library commonMain）依赖的 DAO/Repository
 * 接 :core:common JVM 库（[DesktopContainer.database] 单例，P2b 后数据栈全 KMP）。
 * 仓库实现同为 commonMain RoomXxxRepository，安卓侧 Koin 装配（core:data）同构。
 *
 * U11 扩充：SourceRepository/CredentialsRepository/ErrorLogStore（WebDAV 链路依赖，
 * [webdavCoreModule] 的 AuthRegistry/KtorWebDavClient 构造需要）+ webdavCoreModule +
 * sourcesCoreModule（共享 WebDAV 浏览/表单 ViewModel，桌面复用共享浏览页）。
 */
fun desktopLibraryModule(): Module = module {
    // U12：端口绑定——共享 PlayerViewModel 经 PlaybackPort 消费桌面播放栈
    single<com.muses.player.core.playback.PlaybackPort> { DesktopRuntime.playerHook() }
    single { DesktopContainer.database().songDao() }
    single { DesktopContainer.database().albumDao() }
    single { DesktopContainer.database().artistDao() }
    single { DesktopContainer.database().sourceDao() }
    single<SongRepository> { RoomSongRepository(get(), get(), get()) }
    single<AlbumRepository> { RoomAlbumRepository(get()) }
    single<ArtistRepository> { RoomArtistRepository(get()) }
    single<SourceRepository> { RoomSourceRepository(get()) }
    // WebDAV 凭据（DPAPI，见 [DesktopCredentials]）
    single<CredentialsRepository> { DesktopCredentials() }
    // WebDAV 链路日志（桌面无 CrashHandler 落盘链，环形缓冲即可）
    single<ErrorLogStore> { RingBufferErrorLogStore() }
}

/** 桌面全量模块：共享 ViewModel 装配 + 桌面 DAO/Repository 接线。 */
val desktopAppModules: List<Module> = listOf(
    desktopLibraryModule(),
    libraryModule,
    playerModule,
    webdavCoreModule,
    sourcesCoreModule,
)
