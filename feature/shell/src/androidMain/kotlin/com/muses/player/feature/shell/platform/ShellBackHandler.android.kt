package com.muses.player.feature.shell.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

actual @Composable fun ShellBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
