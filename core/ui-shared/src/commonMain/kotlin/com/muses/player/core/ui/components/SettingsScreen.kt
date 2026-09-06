package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing

/**
 * 跨平台设置页共用组件（U4 设置页共用化；U15 移除内置音源管理区块——
 * 音源有独立模块（feature:sources 音源页），设置页不再重复承载，
 * 历史遗留的「Android 端 emptyList 占位死区块」随之消除）。
 *
 * 纯 UI 容器：吸顶 SaltNavbar + 滚动容器 + 底部 MiniPlayer 避让；
 * - [extraContent]：平台专属扩展区域（Android 放「关于/反馈」，Desktop 留空）。
 *
 * 约束：commonMain 零安卓 import。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    val salt = LocalSaltColors.current

    // ---- navbar 顶部避让 ----
    // 与 SaltNavbar 同口径：CMP WindowInsets 跨平台取真实状态栏高度，桌面返回 0
    val statusBarTop = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val navbarPt = maxOf(SaltSpacing.navbarTopPaddingMin, statusBarTop) + 44.dp

    Box(modifier = modifier.fillMaxSize().background(salt.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = navbarPt + 8.dp),
        ) {
            // ---- 平台扩展区域 ----
            extraContent()

            // ---- 底部避让 MiniPlayer ----
            Spacer(Modifier.height(96.dp))
        }

        // ---- 吸顶导航栏 ----
        SaltNavbar(
            title = "设置",
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// ---- 公共辅助组件（供外部扩展区域复用） ----

/**
 * 设置分组标题 —— 对照 SettingsPage.vue `.m-block-title--default`。
 * （16px 顶距 / sm 字号 / 600 字重 / --m-text-2）。
 */
@Composable
fun SettingsBlockTitle(text: String) {
    val salt = LocalSaltColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (13f * 1.4f).sp,
        letterSpacing = 0.02.sp,
        color = salt.text2,
        modifier = Modifier.padding(start = SaltSpacing.spacing, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * 设置项左侧图标容器 —— 36dp 圆角方形 + primary 浅底。
 * （rgba(var(--m-primary-rgb), 0.12)，明暗主题自动跟随）。
 *
 * 注意 Web margin-right → Compose `padding(end)` 必须放链最外层（先留间距再画壳），
 * 放 size/background 之后会收缩背景本身（布局陷阱 #7）。
 */
@Composable
fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val salt = LocalSaltColors.current
    Box(
        modifier = Modifier
            .padding(end = SaltSpacing.spacingSub)
            .size(36.dp)
            .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(SaltRadius.sm)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = salt.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
