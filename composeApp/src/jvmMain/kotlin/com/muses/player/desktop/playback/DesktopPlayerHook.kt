package com.muses.player.desktop.playback

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.model.lyrics.OnlineLyricsFailReason
import com.muses.player.core.model.lyrics.OnlineLyricsMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.desktop.DesktopScrapeGraph
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.core.model.Song
import com.muses.player.core.model.playback.PlayerConfig
import com.muses.player.core.model.playback.RepeatMode
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.playback.PlaybackStates
import com.muses.player.desktop.playback.JvmPlayerPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 播放页在线歌词搜索态（Y3→U13）：无库歌词时按钮触发，命中后写回曲库
 * （lyrics/lyricsFormat/lyricsSource=online，对齐安卓刮削写回语义），
 * 展示交由曲库实时流（共享 PlayerViewModel 的解析链自动重解析）。
 */
sealed interface DesktopLyricsSearchState {
    data object Idle : DesktopLyricsSearchState
    data object Searching : DesktopLyricsSearchState
    /** 命中并已写回曲库（source = 命中源 wire 名） */
    data class Done(val source: String) : DesktopLyricsSearchState
    data class Failed(val message: String) : DesktopLyricsSearchState
}

/**
 * 桌面播放接线（S3b）：Room 曲库 + JvmPlayerPort + WebDAV 扫描。
 *
 * - 曲库：songDao/sourceDao 直接读库；
 * - 播放：playerPort.enqueue/play；
 * - 扫描：S3b 最小版——按音源 URL 做 PROPFIND 列表并入库（复用 core:webdav Ktor 客户端语义）。
 *   完整扫描（标签解析/增量）随 S5 回归后补，首版只保证建库/扫库/播放链路可用。
 * - 歌词（U13 对齐安卓）：展示链收归曲库字段——共享 PlayerViewModel 经 SongDao 实时流解析
 *   （与安卓刮削写回同源）；[searchOnlineLyrics] 命中后写回曲库
 *   （lyrics/lyricsFormat/lyricsSource=online），原内存展示链已删除。
 */
class DesktopPlayerHook(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PlaybackPort {
    private val db: MusesDatabase by lazy { DesktopContainer.database() }
    private val cache: DesktopWebDavAudioCache by lazy { DesktopContainer.audioCache() }

    private var playerPort: JvmPlayerPort? = null

    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _sources = MutableStateFlow<List<SourceEntity>>(emptyList())
    val sources: StateFlow<List<SourceEntity>> = _sources.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<String?>(null)
    override val currentSongId: StateFlow<String?> = _currentSongId.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // ── U12 端口面（PlaybackPort）：与安卓 PlayerConnection 同契约，托盘/SMTC/共享组件统一消费 ──

    override val duration: StateFlow<Long> = _durationMs.asStateFlow()

    /** 播放状态整型（与 Media3 STATE_* 冻结同值，bridge 自 JvmPlayerPort） */
    private val _playbackState = MutableStateFlow(PlaybackStates.STATE_IDLE)
    override val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    /** 循环/随机配置（bridge 自 JvmPlayerPort.playerConfig） */
    private val _playerConfig = MutableStateFlow(PlayerConfig())
    override val playerConfig: StateFlow<PlayerConfig> = _playerConfig.asStateFlow()

    /** 兜底封面：桌面无实时 metadata 封面流，恒空（TODO：VLCJ cover art 桥接） */
    private val _artworkUri = MutableStateFlow<String?>(null)
    override val artworkUri: StateFlow<String?> = _artworkUri.asStateFlow()

    /** 当前曲实时展示元数据（桌面暂空——展示走曲库实时流；TODO：随 artworkUri 一并桥接） */
    private val _currentMeta = MutableStateFlow<com.muses.player.core.playback.PlaybackMeta?>(null)
    override val currentMeta: StateFlow<com.muses.player.core.playback.PlaybackMeta?> = _currentMeta.asStateFlow()

    /** 播放失败文案（bridge 自 JvmPlayerPort.playbackError；桌面错误仅提示不阻断） */
    private val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    /** 当前队列（play/enqueue 时记录；桌面播放语义 = 列表整体入队） */
    private val _queueSongIds = MutableStateFlow<List<String>>(emptyList())
    override val queueSongIds: StateFlow<List<String>> = _queueSongIds.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _lyricsSearch = MutableStateFlow<DesktopLyricsSearchState>(DesktopLyricsSearchState.Idle)
    val lyricsSearch: StateFlow<DesktopLyricsSearchState> = _lyricsSearch.asStateFlow()

    /** 在线歌词搜索 job（重复点击/切歌取消上一个查询） */
    private var lyricsSearchJob: Job? = null

    suspend fun ensurePlayer(): JvmPlayerPort {
        playerPort?.let { return it }
        val port = DesktopContainer.playerPort()
        playerPort = port
        // 桥接播放器状态到 UI
        scope.launch { port.isPlaying.collect { _isPlaying.value = it } }
        scope.launch { port.currentSongId.collect { _currentSongId.value = it } }
        scope.launch { port.positionMs.collect { _positionMs.value = it } }
        scope.launch { port.durationMs.collect { _durationMs.value = it } }
        scope.launch { port.volume.collect { _volume.value = it } }
        scope.launch { port.playbackState.collect { _playbackState.value = it } }
        scope.launch { port.playerConfig.collect { _playerConfig.value = it } }
        scope.launch { port.playbackError.collect { _playbackError.value = it } }
        return port
    }

    fun refresh() {
        scope.launch {
            runCatching {
                _sources.value = db.sourceDao().observeAll().first()
                _songs.value = db.songDao().getAll()
            }.onFailure { e ->
                _status.value = "读取曲库失败：${e.message}"
            }
        }
    }

    fun play(songId: String) {
        scope.launch {
            runCatching {
                val port = ensurePlayer()
                val ids = _songs.value.map { it.id }
                val index = ids.indexOf(songId).coerceAtLeast(0)
                port.enqueue(ids, index)
                port.play()
                _queueSongIds.value = ids
                _status.value = ""
            }.onFailure { e ->
                _status.value = "播放失败：${e.message}"
            }
        }
    }

    override fun play(songId: String, songs: List<Song>) {
        // 桌面播放语义 = 曲库列表整体入队（songs 参数忽略，见 play(songId)）
        play(songId)
    }

    override fun playPause() = togglePlayPause()

    override fun play() {
        scope.launch { runCatching { ensurePlayer().play() } }
    }

    override fun pause() {
        scope.launch { runCatching { ensurePlayer().pause() } }
    }

    override fun playAtIndex(index: Int) {
        // 桌面队列重设语义：以当前队列重 enqueue 并定位（JvmPlayerPort 无 seekTo(index) 原语）
        scope.launch {
            runCatching {
                val ids = _queueSongIds.value.ifEmpty { _songs.value.map { it.id } }
                val port = ensurePlayer()
                port.enqueue(ids, index)
                port.play()
                _queueSongIds.value = ids
            }
        }
    }

    override fun removeQueueItemAt(index: Int) {
        scope.launch {
            runCatching {
                ensurePlayer().removeQueueItemAt(index)
                _queueSongIds.value = ensurePlayer().activeOrderIds()
            }
        }
    }

    /** U20：按 songId 集合清理队列（删音源同步清队列） */
    override fun removeFromQueue(songIds: Set<String>) {
        scope.launch {
            runCatching {
                ensurePlayer().removeFromQueue(songIds)
                _queueSongIds.value = ensurePlayer().activeOrderIds()
            }
        }
    }

    override fun clearQueueItems() {
        scope.launch {
            runCatching {
                ensurePlayer().clearQueueItems()
                _queueSongIds.value = emptyList()
            }
        }
    }

    override fun skipToNext() = next()

    override fun skipToPrevious() = previous()

    override fun currentPosition(): Long = _positionMs.value

    override fun clearPlaybackError() {
        // TODO：JvmPlayerPort 错误态暂无清除原语（桌面错误仅提示不阻断）
    }

    override fun enqueue(ids: List<String>, index: Int) {
        scope.launch {
            runCatching {
                ensurePlayer().enqueue(ids, index)
                _queueSongIds.value = ids
            }.onFailure { e -> _status.value = "入队失败：${e.message}" }
        }
    }

    override fun resetRecovery() {
        // TODO：桌面无恢复链（WebDAV 限流重试链为安卓侧实现）
    }

    override fun setRepeatMode(mode: Int) {
        scope.launch { runCatching { ensurePlayer().setRepeatMode(mode) } }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        scope.launch { runCatching { ensurePlayer().setRepeatMode(mode) } }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        scope.launch { runCatching { ensurePlayer().setShuffleEnabled(enabled) } }
    }

    fun togglePlayPause() {
        scope.launch {
            runCatching {
                val port = ensurePlayer()
                if (_isPlaying.value) port.pause() else port.play()
            }.onFailure { e ->
                _status.value = "播放失败：${e.message}"
            }
        }
    }

    fun next() {
        scope.launch { runCatching { ensurePlayer().next() } }
    }

    // 端口别名（skipToNext/skipToPrevious 转发见上）

    fun previous() {
        scope.launch { runCatching { ensurePlayer().previous() } }
    }

    override fun seekTo(ms: Long) {
        scope.launch { runCatching { ensurePlayer().seekTo(ms) } }
    }

    fun setVolume(volumePercent: Int) {
        scope.launch { runCatching { ensurePlayer().setVolume(volumePercent) } }
    }

    /**
     * U13：无库歌词时的补充链——LyricsMatcher（AMLL 优先 → 平台五源 → LRCLIB）。
     * 命中后写回曲库（对齐安卓刮削写回：lyrics/lyricsFormat/lyricsSource=online），
     * 曲库实时流自动驱动共享 PlayerViewModel 重解析展示；不再有内存歌词态。
     */
    fun searchOnlineLyrics() {
        val song = _songs.value.firstOrNull { it.id == _currentSongId.value }
        if (song == null) {
            _lyricsSearch.value = DesktopLyricsSearchState.Failed("当前没有正在播放的歌曲")
            return
        }
        lyricsSearchJob?.cancel()
        _lyricsSearch.value = DesktopLyricsSearchState.Searching
        lyricsSearchJob = scope.launch {
            val query = OnlineLyricsQuery(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.albumTitle,
                durationSec = song.durationSec.takeIf { it > 0 }?.toDouble()
                    ?: song.durationMs.takeIf { it > 0 }?.let { it / 1000.0 },
            )
            val next = try {
                when (val result = DesktopScrapeGraph.lyricsMatcher.match(query)) {
                    is OnlineLyricsMatchResult.Ok -> {
                        val document = LyricsParser.parseDocument(result.text)
                        if (document == null || document.lines.isEmpty()) {
                            DesktopLyricsSearchState.Failed("命中歌词解析失败")
                        } else {
                            // U13：写回曲库（安卓刮削写回同语义），展示交给曲库实时流
                            runCatching {
                                val entity = db.songDao().getById(song.id)
                                if (entity != null) {
                                    db.songDao().upsert(
                                        entity.copy(
                                            lyrics = result.text,
                                            lyricsFormat = result.format.wire,
                                            lyricsSource = LyricsSource.ONLINE.wire,
                                        ),
                                    )
                                }
                            }.onFailure { e ->
                                DesktopErrorLog.log("DesktopPlayerHook", "歌词写回曲库失败", e)
                            }
                            DesktopLyricsSearchState.Done(result.source.wire)
                        }
                    }
                    is OnlineLyricsMatchResult.Fail -> DesktopLyricsSearchState.Failed(
                        when (result.reason) {
                            OnlineLyricsFailReason.NETWORK -> "网络异常，请检查网络后重试"
                            OnlineLyricsFailReason.PARSE -> "歌词解析失败"
                            OnlineLyricsFailReason.NO_MATCH -> "未找到匹配歌词"
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DesktopErrorLog.log("DesktopPlayerHook", "在线歌词搜索失败", e)
                DesktopLyricsSearchState.Failed("在线搜索失败：${e.message}")
            }
            if (_lyricsSearch.value is DesktopLyricsSearchState.Searching) {
                _lyricsSearch.value = next
            }
        }
    }

}
