package com.muses.player.core.data.db

/**
 * 桌面 actual：bundled SQLite 单写连接且播放列表链路桌面暂无消费方，直行执行。
 * 显式事务待 Room KMP withTransaction 覆盖 JVM 后替换（U10 备忘）。
 */
internal actual suspend fun <R> MusesDatabase.withTransactionCompat(block: suspend () -> R): R =
    block()
