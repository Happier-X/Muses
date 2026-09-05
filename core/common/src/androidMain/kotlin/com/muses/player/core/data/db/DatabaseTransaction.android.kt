package com.muses.player.core.data.db

import androidx.room.withTransaction

/** 安卓 actual：room-ktx 真事务（与原 :core:data 实现语义一致）。 */
internal actual suspend fun <R> MusesDatabase.withTransactionCompat(block: suspend () -> R): R =
    withTransaction { block() }
