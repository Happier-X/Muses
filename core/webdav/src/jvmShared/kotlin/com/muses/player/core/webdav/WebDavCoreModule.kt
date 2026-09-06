package com.muses.player.core.webdav

import com.muses.player.core.media.scanner.WebDavLibraryScanner
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * WebDAV 跨平台装配（U11 KMP 拆分）：AuthRegistry + Ktor 客户端 + 共享限流器，
 * android 与桌面 jvm 双端共用；OkHttp 流播绑定/磁盘缓存见 androidMain [webdavModule]。
 *
 * U25 扩充：WebDAV 库扫描器（上收自 :core:media，android/desktop 双端共用，
 * BFS 递归 PROPFIND + 文件名建库，零下载）。
 */
val webdavCoreModule = module {

    singleOf(::WebDavAuthRegistry)

    // P2c：Ktor 实现。HttpClient 用类内默认 CIO 客户端；显式工厂避免 Koin 误解析 HttpClient 绑定。
    single<WebDavClient> {
        KtorWebDavClient(
            authRegistry = get(),
            rateLimiter = get(),
            errorLogStore = get(),
        )
    }

    singleOf(::WebDavLibraryScanner)

    /** 共享限流器：WebDAV 播放 + 刮削全局 4 rps，供 OkHttp 流播与 WebDavClient 双链路共享 */
    single { WebDavRateLimiter() }
}
