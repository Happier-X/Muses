package com.muses.player.core.data.repository

import com.muses.player.core.data.dao.PlaylistDao
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.PlaylistEntity
import com.muses.player.core.data.db.PlaylistSongEntity
import com.muses.player.core.data.db.withTransactionCompat
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.Playlist
import com.muses.player.core.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Room 播放列表仓库（U10 从 :core:data 上收至 jvmShared：withTransaction/UUID/System
 * 依赖需 JVM 面，android 与桌面 jvm 双端共用；包名不变，安卓 Koin 装配零改动）。
 */
class RoomPlaylistRepository constructor(
    private val db: MusesDatabase,
) : PlaylistRepository {

    private val dao: PlaylistDao = db.playlistDao()

    override fun observeValidCounts(): Flow<Map<String, Int>> =
        dao.observeValidCounts().map { list -> list.associate { it.playlistId to it.validCount } }

    override fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylists().map { list -> list.map { it.toDomain() } }

    override fun observePlaylist(id: String): Flow<PlaylistWithSongs?> =
        combine(dao.observeById(id), dao.observeSongsWithSong(id)) { playlist, rows ->
            playlist?.let {
                PlaylistWithSongs(
                    playlist = it.toDomain(),
                    songs = rows.mapNotNull { row -> row.song?.toDomain() },
                )
            }
        }

    override fun observePlaylistSongIds(id: String): Flow<List<String>> =
        dao.observeSongIds(id)

    override suspend fun getSongs(id: String): List<com.muses.player.core.model.Song> =
        dao.observeSongsWithSong(id).first().mapNotNull { it.song?.toDomain() }


    override suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.withTransactionCompat {
            dao.insert(PlaylistEntity(id = id, name = name, createdAt = now, updatedAt = now))
        }
        return id
    }

    override suspend fun renamePlaylist(id: String, name: String) {
        dao.rename(id, name, System.currentTimeMillis())
    }

    override suspend fun deletePlaylist(id: String) {
        dao.deleteById(id)
    }

    override suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        if (songIds.isEmpty()) return
        db.withTransactionCompat {
            val existing = dao.getSongIds(playlistId).toHashSet()
            var next = (dao.maxPosition(playlistId) ?: -1) + 1
            val toAdd = songIds.distinct()
                .filterNot { it in existing }
                .map { songId -> PlaylistSongEntity(playlistId, songId, next++) }
            if (toAdd.isNotEmpty()) {
                dao.appendSongs(toAdd)
                dao.touch(playlistId, System.currentTimeMillis())
            }
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        db.withTransactionCompat {
            dao.removeSongAndCompact(playlistId, songId)
            dao.touch(playlistId, System.currentTimeMillis())
        }
    }

    override suspend fun moveSong(playlistId: String, fromPosition: Int, toPosition: Int) {
        db.withTransactionCompat {
            dao.moveSong(playlistId, fromPosition, toPosition)
            dao.touch(playlistId, System.currentTimeMillis())
        }
    }
}
