package com.muses.player.feature.player.platform

import androidx.compose.runtime.Composable

/**
 * 文本分享句柄（U21）：歌词长按分享抽为 expect/actual。
 * 返回平台分享回调——安卓唤起系统分享面板（Intent.createChooser），
 * 桌面写系统剪贴板（AWT Toolkit）。
 */
@Composable
expect fun rememberShareTextHandler(): (String) -> Unit
