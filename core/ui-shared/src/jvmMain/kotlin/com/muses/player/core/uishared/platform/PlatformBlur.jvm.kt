package com.muses.player.core.uishared.platform

/**
 * U2 jvmMain（桌面）真实现：Haze 2.0 为 KMP 工件（jvm 变体已发布，Skia 渲染），
 * 桌面与安卓同走真模糊，组件层无需降级分支。
 *
 * - [enabled] = true，半径对齐 Web `backdrop-filter: blur(20px)` 与安卓侧一致；
 * - HazeState 由桌面壳（MusesDesktopApp）创建并提供 `LocalHazeBlurState`；
 *   未提供（null）时 [platformBlurModifier] 仍回退纯色背景，行为安全。
 */
actual object PlatformBlur {
    actual val enabled: Boolean = true
    actual val radiusDp: Float = 20f
}
