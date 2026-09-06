package com.muses.player.feature.player.platform

import android.os.SystemClock

actual fun platformRealtimeMs(): Long = SystemClock.elapsedRealtime()
