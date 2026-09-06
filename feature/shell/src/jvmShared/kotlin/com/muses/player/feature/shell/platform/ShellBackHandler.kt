package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

/**
 * 系统返回拦截（U22 expect/actual）：CMP ui 的 backhandler 在当前 compose 版本
 * 不可用，安卓走 androidx.activity.compose.BackHandler，桌面无返回键空实现。
 */
@Composable
expect fun ShellBackHandler(enabled: Boolean = true, onBack: () -> Unit)
