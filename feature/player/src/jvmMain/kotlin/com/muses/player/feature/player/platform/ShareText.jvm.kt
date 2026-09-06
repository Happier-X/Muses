package com.muses.player.feature.player.platform

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual @Composable fun rememberShareTextHandler(): (String) -> Unit {
    return { text ->
        // 桌面无系统分享面板：歌词分享写系统剪贴板（与 U17 设置页复制日志同语义）
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
