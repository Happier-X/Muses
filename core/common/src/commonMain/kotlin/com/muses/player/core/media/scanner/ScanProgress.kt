package com.muses.player.core.media.scanner

/** 扫描进度（total=0 表示尚未开始/正在枚举；U11 从 :core:media 下沉，同包名安卓侧零改动） */
data class ScanProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFile: String? = null,
    /** 终态标记：成功完成或失败 */
    val finished: Boolean = false,
)
