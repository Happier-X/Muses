package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.desktop.playback.DesktopLyricsSearchState
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.feature.player.PlayerViewModel
import com.muses.player.feature.player.lyric.SimpleLyricsPanel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 桌面播放页（U13 对齐安卓）：数据源收归共享 [PlayerViewModel]（commonMain）——
 * 播放状态/进度/歌词解析/翻译开关与安卓同一链路（曲库 lyrics 字段实时流）；
 * 控制经端口（playback = DesktopRuntime 单例 hook，托盘/SMTC 状态同源）。
 * 桌面特性保留：音量（hook 直调）、「在线搜索」按钮（LyricsMatcher 命中写回曲库，
 * 与安卓刮削写回同语义，写回后曲库实时流自动驱动 VM 重解析展示）。
 */
@Composable
fun PlayerScreen(playerHook: DesktopPlayerHook?) {
    val hook = remember { playerHook ?: DesktopRuntime.playerHook() }
    val vm: PlayerViewModel = koinViewModel()

    val isPlaying by vm.isPlaying.collectAsState()
    val positionMs by vm.position.collectAsState()
    val durationMs by vm.duration.collectAsState()
    val currentSong by vm.currentSong.collectAsState()
    val parsedLines by vm.parsedLines.collectAsState()
    val lyricPosition by vm.lyricPosition.collectAsState()
    val translationEnabled by vm.translationEnabled.collectAsState()
    val hasTranslation by vm.hasTranslation.collectAsState()
    val status by hook.status.collectAsState()
    val lyricsSearch by hook.lyricsSearch.collectAsState()
    val volume by hook.volume.collectAsState()

    Row(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 左面板：封面 + 控制（控制行为与 S3b 单栏版一致，改走端口）
        Column(
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "正在播放",
                color = Color(0xFF7F849C),
                fontSize = 13.sp,
            )
            // 封面占位（首版无 Coil 图片链，纯色块 + 首字）
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF313244)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = currentSong?.title?.firstOrNull()?.toString() ?: "♪",
                    color = Color(0xFF585B70),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // 标题区（曲库实时流，metaTitle/metaArtist 刮削优先，与安卓播放页同口径）
            Text(
                text = currentSong?.let { it.metaTitle ?: it.title } ?: "未在播放",
                color = Color(0xFFCDD6F4),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            val subtitle = listOfNotNull(currentSong?.metaArtist ?: currentSong?.artist, currentSong?.albumTitle)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = Color(0xFF7F849C), fontSize = 14.sp)
            }
            // 进度条（VM 500ms 轮询）
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                var sliderValue by remember(positionMs) { mutableStateOf(positionMs.toFloat()) }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { vm.seekTo(sliderValue.toLong()) },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatMs(positionMs), color = Color(0xFF7F849C), fontSize = 12.sp)
                    Text(text = formatMs(durationMs), color = Color(0xFF7F849C), fontSize = 12.sp)
                }
            }
            // 控制栏（端口）
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton("⏮", 44.dp) { vm.skipToPrevious() }
                ControlButton(if (isPlaying) "⏸" else "▶", 64.dp, primary = true) {
                    vm.playPause()
                }
                ControlButton("⏭", 44.dp) { vm.skipToNext() }
            }
            // 音量（桌面特性：端口无音量面，hook 直调）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(text = "音量", color = Color(0xFF7F849C), fontSize = 13.sp)
                var vol by remember(volume) { mutableStateOf(volume.toFloat()) }
                Slider(
                    value = vol,
                    onValueChange = { vol = it },
                    onValueChangeFinished = { hook.setVolume(vol.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "$volume", color = Color(0xFF7F849C), fontSize = 13.sp)
            }
            if (status.isNotBlank()) {
                Text(text = status, color = Color(0xFFF38BA8), fontSize = 13.sp)
            }
        }
        // 分隔线
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF313244)),
        )
        // 右面板：歌词（共享 VM 解析链：曲库 lyrics 字段 → AMLL 行，100ms 卡拉OK进度）
        Box(
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (parsedLines.isEmpty()) {
                LyricsEmptyState(
                    hasSong = currentSong != null,
                    searchState = lyricsSearch,
                    onSearch = hook::searchOnlineLyrics,
                )
            } else {
                SimpleLyricsPanel(
                    lines = parsedLines,
                    positionMs = lyricPosition,
                    isPlaying = isPlaying,
                    onSeek = { vm.seekTo(it) },
                    modifier = Modifier.fillMaxSize(),
                    // U13：翻译开关接共享 VM 真实值（SimpleLyricsPanel 兼容参数）
                    translationEnabled = translationEnabled,
                    hasTranslation = hasTranslation,
                    onToggleTranslation = { vm.toggleTranslation() },
                )
            }
        }
    }
}

/** 歌词空态：提示 + 在线搜索按钮（命中写回曲库；搜索中/失败/成功态就地反馈） */
@Composable
private fun LyricsEmptyState(
    hasSong: Boolean,
    searchState: DesktopLyricsSearchState,
    onSearch: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "暂无歌词",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
        )
        when (searchState) {
            is DesktopLyricsSearchState.Searching -> {
                CircularProgressIndicator(
                    color = Color(0xFF89B4FA),
                    modifier = Modifier.size(22.dp),
                )
                Text(text = "正在在线搜索歌词…", color = Color(0xFF7F849C), fontSize = 13.sp)
            }
            is DesktopLyricsSearchState.Done -> {
                // U13：命中已写回曲库；曲库实时流刷新后即切歌词展示
                Text(
                    text = "已从${lyricsSourceLabel(searchState.source)}写入曲库",
                    color = Color(0xFFA6E3A1),
                    fontSize = 13.sp,
                )
            }
            is DesktopLyricsSearchState.Failed -> {
                Text(text = searchState.message, color = Color(0xFFF38BA8), fontSize = 13.sp)
                SearchButton(enabled = hasSong, onSearch = onSearch)
            }
            is DesktopLyricsSearchState.Idle -> {
                SearchButton(enabled = hasSong, onSearch = onSearch)
            }
        }
    }
}

/** 在线命中源 wire → 友好名（amll/kw/tx/wy/kg/mg/lrclib） */
private fun lyricsSourceLabel(wire: String): String = when (wire) {
    "amll" -> "AMLL TTML"
    "kw" -> "酷我"
    "tx" -> "QQ音乐"
    "wy" -> "网易云"
    "kg" -> "酷狗"
    "mg" -> "咪咕"
    "lrclib" -> "LRCLIB"
    else -> wire
}

@Composable
private fun SearchButton(enabled: Boolean, onSearch: () -> Unit) {
    Button(
        onClick = onSearch,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF89B4FA),
            contentColor = Color(0xFF11111B),
        ),
    ) {
        Text(text = "在线搜索")
    }
}

@Composable
private fun ControlButton(
    text: String,
    size: androidx.compose.ui.unit.Dp,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) Color(0xFF89B4FA) else Color(0xFF313244))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color(0xFF11111B) else Color(0xFFCDD6F4),
            fontSize = if (primary) 24.sp else 18.sp,
        )
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
