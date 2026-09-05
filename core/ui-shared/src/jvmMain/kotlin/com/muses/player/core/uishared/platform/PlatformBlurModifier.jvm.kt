package com.muses.player.core.uishared.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.theme.HazeBlurStyleData
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * U2 jvmMain（桌面）actual：Haze 真模糊（Skia）。
 * 与安卓 actual 同构——haze 2.0 KMP 统一 API（HazeInput/HazeBlurStyle/hazeBlur）双端一致；
 * [hazeState] 为 null（桌面壳未提供）时回退半透明纯色背景。
 */
@Composable
@ReadOnlyComposable
actual fun platformBlurModifier(
    isDark: Boolean,
    backgroundColor: Color,
    hazeState: Any?,
    hazeStyleData: HazeBlurStyleData?,
): Modifier {
    // 类型擦除桥接：commonMain 传入 Any?，桌面 actual 转为 HazeState
    @Suppress("UNCHECKED_CAST")
    val hzState = hazeState as? dev.chrisbanes.haze.HazeState

    return if (hzState != null && hazeStyleData != null) {
        // 将跨平台风格数据转换为 HazeBlurStyle
        val hazeStyle = HazeBlurStyle(
            backgroundColor = hazeStyleData.backgroundColor,
            colorEffects = listOf(
                HazeColorEffect.tint(hazeStyleData.tint),
            ),
            blurRadius = hazeStyleData.blurRadiusDp.dp,
            noiseFactor = 0.01f,
        )
        Modifier.hazeBlur(input = HazeInput.Sources(hzState), style = hazeStyle)
    } else {
        Modifier.background(color = backgroundColor)
    }
}
