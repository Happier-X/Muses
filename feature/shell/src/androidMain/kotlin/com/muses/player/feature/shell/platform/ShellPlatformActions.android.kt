package com.muses.player.feature.shell.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual @Composable fun rememberShellPlatformActions(): ShellPlatformActions {
    val context = LocalContext.current
    return ShellPlatformActions(
        openUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        copyToClipboard = { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Muses 报错日志", text))
        },
    )
}
