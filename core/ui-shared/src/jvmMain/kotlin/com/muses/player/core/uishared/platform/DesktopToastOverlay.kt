package com.muses.player.core.uishared.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 桌面 Toast 浮层（U2）：消费 [PlatformToast] 消息总线，底部居中短提示，
 * 2.2s 自动消退。对照安卓 Toast.LENGTH_SHORT 的停留节奏；样式取 Salt 深色胶囊
 * （与桌面壳 MusesDesktopApp 的 Catppuccin 底色一致）。
 *
 * 覆盖层不参与点击命中（无 pointerInput），浮层显示期间交互照常穿透。
 * 由桌面壳在内容顶层挂载一次：`DesktopToastOverlay()`。
 */
@Composable
fun DesktopToastOverlay(modifier: Modifier = Modifier) {
    val message by desktopToastMessage.collectAsState()
    var visibleText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            visibleText = message
            delay(2_200)
            visibleText = null
            desktopToastMessage.value = null
        }
    }

    visibleText?.let { text ->
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC181825))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = text,
                    color = Color(0xFFCDD6F4),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
