package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

actual @Composable fun rememberShellPlatformActions(): ShellPlatformActions {
    return ShellPlatformActions(
        openUrl = { url ->
            runCatching { Desktop.getDesktop().browse(URI(url)) }
        },
        copyToClipboard = { text ->
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        },
    )
}
