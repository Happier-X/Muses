package com.muses.player.core.data.store

actual fun platformNowIso(): String = java.time.Instant.now().toString()

actual fun platformNowMs(): Long = System.currentTimeMillis()

actual fun platformMonotonicMs(): Long = System.nanoTime() / 1_000_000L
