package com.muses.player.feature.player.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual @Composable fun rememberShareTextHandler(): (String) -> Unit {
    val context = LocalContext.current
    return { text ->
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text),
                "分享歌词",
            ),
        )
    }
}
