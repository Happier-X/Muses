package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

/**
 * 启动期权限申请（U22 expect/actual）：安卓在首帧申请 READ_MEDIA_AUDIO/POST_NOTIFICATIONS
 * （拒绝静默，与原 MusesApp 行为一致）；桌面无运行时权限，空实现。
 */
@Composable
expect fun PermissionsEffect()
