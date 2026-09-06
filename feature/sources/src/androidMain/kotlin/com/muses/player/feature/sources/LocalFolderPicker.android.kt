package com.muses.player.feature.sources

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 安卓实现：SAF OpenDocumentTree 系统目录选择器。
 * 选中后持久化读写授权（重启后扫描/播放仍可访问），并解析出物理绝对路径
 * （primary:Music → /storage/emulated/0/Music；XXXX-XXXX:dir → /storage/XXXX-XXXX/dir），
 * 供 LocalLibraryScanner 的 MediaStore DATA 前缀过滤直接使用。
 */
@Composable
actual fun rememberLocalFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            // 持久化读写权限：重启后扫描/播放仍可访问该目录
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            resolvePhysicalPath(treeUri)?.let(onPicked)
        }
    }

    return { dirPickerLauncher.launch(null) }
}

/** DocumentsContract 文档 id → 物理路径（externalstorage provider 标准格式） */
private fun resolvePhysicalPath(treeUri: Uri): String? =
    runCatching {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val (volume, subPath) = docId.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        when {
            volume.equals("primary", ignoreCase = true) ->
                "/storage/emulated/0" + if (subPath.isNotEmpty()) "/$subPath" else ""
            else -> "/storage/$volume" + if (subPath.isNotEmpty()) "/$subPath" else ""
        }
    }.getOrNull()
