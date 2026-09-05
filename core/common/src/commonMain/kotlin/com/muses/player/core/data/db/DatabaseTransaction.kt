package com.muses.player.core.data.db

/**
 * 跨平台 Room 事务包装（U10 PlaylistRepository 下沉 jvmShared 所需）：
 * androidx.room.withTransaction 是 room-ktx 的 Android 专属 API，JVM 侧无此工件。
 * 安卓 actual 走真事务；桌面 actual 直行（见 jvmMain 注释）。
 */
internal expect suspend fun <R> MusesDatabase.withTransactionCompat(block: suspend () -> R): R
