package com.muses.player.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.appupdate.checkLatestRelease
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.ui.components.SettingsAboutFeedbackContent
import com.muses.player.core.ui.components.SettingsScreen
import com.muses.player.feature.shell.platform.AppVersionProvider
import com.muses.player.feature.shell.platform.rememberShellPlatformActions
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
        return "[Muses 错误日志] v${versionName()} @ $time\n$body"
    }

    /** 版本号经 Koin [AppVersionProvider] 注入（原 BuildConfig 仅安卓可读） */
    private fun versionName(): String =
        org.koin.core.context.GlobalContext.get().get<AppVersionProvider>().versionName
}

/**
 * 设置页（U22 双端共享）：ui-shared 共享容器 + 共享「关于/反馈」扩展区块；
 * 平台动作（浏览器/剪贴板）经 rememberShellPlatformActions 注入，版本号经
 * Koin AppVersionProvider，报错日志同源 ErrorLogStore（桌面绑定 RingBufferErrorLogStore）。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val actions = rememberShellPlatformActions()
    val versionProvider = koinInject<AppVersionProvider>()

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
                    versionName = versionProvider.versionName,
                    onOpenUrl = actions.openUrl,
                    onCopyToClipboard = actions.copyToClipboard,
                    onCheckUpdate = { current -> checkLatestRelease(current) },
                    errorLogSummary = latestSummary,
                    onDumpLogs = { viewModel.dumpLogs() },
                )
            },
        )
    }
}
