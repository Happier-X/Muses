package com.muses.player.feature.player.platform

actual fun platformRealtimeMs(): Long = System.nanoTime() / 1_000_000L
