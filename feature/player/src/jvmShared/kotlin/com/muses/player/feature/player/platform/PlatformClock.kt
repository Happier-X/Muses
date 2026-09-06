package com.muses.player.feature.player.platform

/**
 * 平台单调时钟（U21）：歌词锚点外推用（锚点时刻 + 时钟差 = 实时播放位置）。
 * 语义对齐 android.os.SystemClock.elapsedRealtime()（含休眠的单调毫秒时钟），
 * 桌面以 System.nanoTime 等价实现。
 */
expect fun platformRealtimeMs(): Long
