package com.muses.player.feature.sources

import androidx.compose.runtime.Composable

/**
 * 本地文件夹选择器（U20 expect/actual）：原安卓 SAF OpenDocumentTree 抽象为跨平台端口。
 * 返回「发起选择」回调；用户确认后以平台物理路径回调 [onPicked]
 * （安卓 = SAF tree uri 解析出的绝对路径前缀；桌面 = 文件系统原生路径）。
 * 取消/解析失败时不回调（对齐 Web FilePicker 取消语义）。
 */
@Composable
expect fun rememberLocalFolderPicker(onPicked: (String) -> Unit): () -> Unit
