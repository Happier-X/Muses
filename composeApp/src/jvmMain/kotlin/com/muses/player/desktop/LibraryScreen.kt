package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.components.LibraryTab
import com.muses.player.core.ui.components.LibraryTabBar
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.feature.library.AlbumsScreen
import com.muses.player.feature.library.ArtistsScreen
import com.muses.player.feature.library.SongsScreen

/**
 * 桌面库房页（U9 共用化）：仅承担标签页装配，曲目/专辑/艺术家三屏与 ViewModel
 * 直接复用 :feature:library commonMain 的共享实现（原手搓 DAO 读取已删）。
 * 播放回调接 DesktopPlayerHook；「加入播放列表」slot 桌面暂不传（无播放列表 UI）。
 * 进入时 hook.refresh() 保持原行为（托盘/SMTC 侧 hook.songs 同步）。
 */
@Composable
fun LibraryScreen(playerHook: DesktopPlayerHook?) {
    val salt = LocalSaltColors.current
    val hook = remember { playerHook ?: DesktopPlayerHook() }
    var tab by remember { mutableStateOf(LibraryTab.Songs) }

    LaunchedEffect(Unit) {
        hook.refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(salt.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "曲库",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        LibraryTabBar(
            selected = tab,
            onTabSelect = { tab = it },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                LibraryTab.Songs -> SongsScreen(
                    onPlaySong = { songId, _ -> hook.play(songId) },
                )
                LibraryTab.Albums -> AlbumsScreen(
                    // 桌面详情页二期（与原行为一致）
                    onAlbumClick = {},
                )
                LibraryTab.Artists -> ArtistsScreen(
                    onArtistClick = {},
                )
            }
        }
    }
}
