package com.muses.player.feature.sources

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.data.store.platformNowMs
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.core.media.scanner.LocalLibraryScanner
import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.media.scanner.WebDavLibraryScanner
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.webdav.WebDavAuthRegistry
import com.muses.player.core.webdav.WebDavClient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * [LibraryScanPort] 安卓实现（U11）：本地 MediaStore 扫描器 / WebDAV 扫描器按音源类型分派
 * （原 SourcesViewModel 内联分派逻辑原样平移，两扫描器 scan 语义差异见各自实现）。
 */
class AndroidLibraryScanPort(
    private val scanner: LocalLibraryScanner,
    private val webDavScanner: WebDavLibraryScanner,
) : LibraryScanPort {

    override fun progressFor(type: SourceType): Flow<ScanProgress> =
        if (type == SourceType.WEBDAV) webDavScanner.scanProgress else scanner.scanProgress

    override suspend fun scan(source: Source, readTags: Boolean): List<com.muses.player.core.model.Song> =
        if (source.type == SourceType.WEBDAV) {
            webDavScanner.scan(source)   // WebDAV：纯文件名建库，标签播放时懒扫描
        } else {
            scanner.scan(source, readTags = readTags)
        }
}

/**
 * 音源 ViewModel（安卓侧）：数据核 [SourcesCoreViewModel]（commonMain）
 * + 安卓媒体栈扩展——MediaStore/WebDAV 扫描器经 [AndroidLibraryScanPort] 装配、
 * PlayerConnection 播放队列清理回调、SAF 树 uri 建源。
 */
@OptIn(ExperimentalUuidApi::class)
class SourcesViewModel constructor(
    sourceRepository: SourceRepository,
    songRepository: SongRepository,
    scanner: LocalLibraryScanner,
    webDavScanner: WebDavLibraryScanner,
    settingsRepository: SettingsRepository,
    songDao: com.muses.player.core.data.dao.SongDao,
    scrapeQueueStore: ScrapeQueueStore,
    credentialsRepository: CredentialsRepository,
    webDavClient: WebDavClient,
    webDavAuthRegistry: WebDavAuthRegistry,
    playbackStateRepository: PlaybackStateRepository,
    recentPlaysRepository: RecentPlaysRepository,
    private val playerConnection: PlayerConnection,
) : SourcesCoreViewModel(
    sourceRepository = sourceRepository,
    songRepository = songRepository,
    scanPort = AndroidLibraryScanPort(scanner, webDavScanner),
    settingsRepository = settingsRepository,
    songDao = songDao,
    scrapeQueueStore = scrapeQueueStore,
    credentialsRepository = credentialsRepository,
    webDavClient = webDavClient,
    webDavAuthRegistry = webDavAuthRegistry,
    playbackStateRepository = playbackStateRepository,
    recentPlaysRepository = recentPlaysRepository,
    onRemoveFromQueue = playerConnection::removeFromQueue,
) {

    /**
     * 从 SAF tree uri 建本地源：解析出物理绝对路径前缀存入 Source.path，
     * 供 LocalLibraryScanner 的 MediaStore DATA 前缀过滤直接使用。
     * primary:Music → /storage/emulated/0/Music；XXXX-XXXX:dir → /storage/XXXX-XXXX/dir
     */
    fun saveLocalSourceFromTreeUri(treeUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val physicalPath = resolvePhysicalPath(treeUri, context)
                    ?: return@launch
                val displayName = treeUri.lastPathSegment
                    ?.substringAfterLast(':')
                    ?.substringAfterLast('/')
                    ?: "本地文件夹"
                val now = platformNowMs()
                sourceRepository.upsert(
                    Source(
                        id = Uuid.random().toString(),
                        name = displayName,
                        type = SourceType.LOCAL,
                        path = physicalPath,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } catch (_: Exception) {
                // 选择器取消或解析失败静默（对齐 Web FilePicker 取消语义）
            }
        }
    }

    /** DocumentsContract 文档 id → 物理路径（externalstorage provider 标准格式） */
    private fun resolvePhysicalPath(treeUri: Uri, context: Context): String? =
        runCatching {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val (volume, subPath) = docId.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            when {
                volume.equals("primary", ignoreCase = true) ->
                    "/storage/emulated/0" + if (subPath.isNotEmpty()) "/$subPath" else ""
                else -> "/storage/$volume" + if (subPath.isNotEmpty()) "/$subPath" else ""
            }
        }.getOrNull()
}
