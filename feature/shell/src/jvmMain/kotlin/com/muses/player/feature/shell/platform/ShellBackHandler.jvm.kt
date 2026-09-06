package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

actual @Composable fun ShellBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // 桌面无系统返回键：空实现
}
