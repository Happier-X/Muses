package com.muses.player.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.appupdate.checkLatestRelease
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.ui.components.SettingsAboutFeedbackContent
import com.muses.player.core.ui.components.SettingsScreen
import com.muses.player.BuildConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel constructor(
    private val errorLogStore: ErrorLogStore,
) : ViewModel() {

    /** 最近错误摘要 —— 供「复制报错日志」条目副标题 */
    val latestErrorSummary: StateFlow<String?> = errorLogStore.latestSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 复制用日志全文：文件头（版本 + 导出时间）+ 缓冲正文（含上次会话崩溃段）。
     * 无任何日志时返回 null，由 UI 层提示「暂无可复制的日志」。
     */
    suspend fun dumpLogs(): String? {
        val body = errorLogStore.dump() ?: return null
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return "[Muses 错误日志] v${BuildConfig.VERSION_NAME} @ $time\n$body"
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    val hazeState = rememberHazeState()
    CompositionLocalProvider(
        // 09-05 T2：MusesHaze 下线，统一经 ui-shared 的 LocalHazeBlurState 桥接
        com.muses.player.core.ui.theme.LocalHazeBlurState provides hazeState,
    ) {
        // U15：设置页共享组件（音源区块已移除，独立音源页承载）；「关于/反馈」扩展区为
        // 双端共享实现（SettingsAboutFeedbackContent），平台动作经回调注入。
        SettingsScreen(
            modifier = modifier,
            extraContent = {
                val latestSummary by viewModel.latestErrorSummary.collectAsState()
                SettingsAboutFeedbackContent(
                    versionName = BuildConfig.VERSION_NAME,
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onCopyToClipboard = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Muses 报错日志", text))
                    },
                    onCheckUpdate = { current -> checkLatestRelease(current) },
                    errorLogSummary = latestSummary,
                    onDumpLogs = { viewModel.dumpLogs() },
                )
            },
        )
    }
}
