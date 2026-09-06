package com.muses.player.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.ui.components.SettingsAboutFeedbackContent
import com.muses.player.core.ui.components.SettingsScreen
import org.koin.compose.koinInject

/**
 * 桌面设置页（U15）：与安卓同构——ui-shared 共享容器 + 共享「关于/反馈」扩展区块；
 * 平台动作（浏览器/剪贴板）经 DesktopRuntime 注入，报错日志同源 ErrorLogStore
 * （桌面 Koin 绑定 RingBufferErrorLogStore）。
 */
@Composable
fun SettingsScreen() {
    val errorLog = koinInject<ErrorLogStore>()
    val latestSummary by errorLog.latestSummary.collectAsState()

    SettingsScreen(
        extraContent = {
            SettingsAboutFeedbackContent(
                // U15：运行时版本（构建期资源注入，与 jpackage packageVersion 同源）
                versionName = DesktopRuntime.appVersion(),
                onOpenUrl = { DesktopRuntime.openUrl(it) },
                onCopyToClipboard = { DesktopRuntime.copyToClipboard(it) },
                onCheckUpdate = { current -> com.muses.player.core.appupdate.checkLatestRelease(current) },
                errorLogSummary = latestSummary,
                onDumpLogs = { errorLog.dump() },
            )
        },
    )
}
