package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

/**
 * 壳层平台动作（U22 expect/actual）：设置页「关于/反馈」的消费面。
 * 安卓 = Intent ACTION_VIEW 浏览器 + 系统 ClipboardManager；桌面 = java.awt.Desktop
 * 浏览器 + AWT 剪贴板。版本号不在此处——经 Koin [AppVersionProvider] 绑定注入
 * （安卓 = :app BuildConfig，桌面 = DesktopRuntime 构建期资源）。
 */
class ShellPlatformActions(
    val openUrl: (String) -> Unit,
    val copyToClipboard: (String) -> Unit,
)

@Composable
expect fun rememberShellPlatformActions(): ShellPlatformActions

/** 应用版本号提供者（Koin 绑定，双端各自装配） */
interface AppVersionProvider {
    val versionName: String
}
