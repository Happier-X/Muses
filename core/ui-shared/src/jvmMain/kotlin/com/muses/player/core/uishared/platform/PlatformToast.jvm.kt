package com.muses.player.core.uishared.platform

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * U2 jvmMain（桌面）实现：桌面无系统 Toast 通道，[PlatformToast.show] 写入内部总线，
 * 由桌面壳（MusesDesktopApp）顶层挂载的 [DesktopToastOverlay] 消费渲染为底部浮层；
 * 壳层未挂载浮层时消息留在总线（下次挂载消费一次），不丢功能语义。
 */
internal val desktopToastMessage = MutableStateFlow<String?>(null)

actual object PlatformToast {
    actual fun show(message: String) {
        desktopToastMessage.value = message
    }
}
