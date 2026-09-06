package com.muses.player.feature.sources

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * 桌面实现：Swing JFileChooser 目录选择（U20）。
 * 选择对话框为模态阻塞调用，按 Swing 惯例投递到 AWT EDT 上执行；
 * 确认后回调所选目录的文件系统绝对路径，取消不回调。
 */
@Composable
actual fun rememberLocalFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    return {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "选择音乐文件夹"
                isMultiSelectionEnabled = false
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.absolutePath?.let(onPicked)
            }
        }
    }
}
