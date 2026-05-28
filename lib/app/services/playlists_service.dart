import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite/sqflite.dart';

import '../utils/cache_version_store.dart';
import 'db/db_constants.dart';
import 'db/db_helper.dart';

class PlaylistEntity {
  final String id;
  final String name;
  final List<String> songIds;
  final int createdAtMs;

  const PlaylistEntity({
    required this.id,
    required this.name,
    required this.songIds,
    required this.createdAtMs,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'songIds': songIds,
    'createdAtMs': createdAtMs,
  };

  factory PlaylistEntity.fromJson(Map<String, dynamic> json) {
    final rawSongIds = json['songIds'];
    final songIds = rawSongIds is List
        ? rawSongIds
              .map((e) => e?.toString() ?? '')
              .where((e) => e.isNotEmpty)
              .toList()
        : const <String>[];
    return PlaylistEntity(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      songIds: songIds,
      createdAtMs: int.tryParse((json['createdAtMs'] ?? '').toString()) ?? 0,
    );
  }

  PlaylistEntity copyWith({
    String? id,
    String? name,
    List<String>? songIds,
    int? createdAtMs,
  }) {
    return PlaylistEntity(
      id: id ?? this.id,
      name: name ?? this.name,
      songIds: songIds ?? this.songIds,
      createdAtMs: createdAtMs ?? this.createdAtMs,
    );
  }
}

class PlaylistsService {
  static final PlaylistsService instance = PlaylistsService._internal();

  static const String _prefsKey = 'playlists_v1';
  static const String cacheVersionScope = 'playlists';

  PlaylistsService._internal();

  void _bumpCacheVersion() {
    CacheVersionStore.instance.bump(cacheVersionScope);
  }

  Future<List<PlaylistEntity>> loadAll() async {
    final db = await DbHelper.instance.database;
    var rows = await db.query(
      DbConstants.tablePlaylists,
      orderBy: 'sortOrder ASC',
    );
    if (rows.isEmpty) {
      final migrated = await _migrateFromPrefs(db);
      if (migrated) {
        rows = await db.query(
          DbConstants.tablePlaylists,
          orderBy: 'sortOrder ASC',
        );
      }
    }
    final songRows = await db.query(
      DbConstants.tablePlaylistSongs,
      orderBy: 'sortOrder ASC',
    );
    final songsMap = <String, List<String>>{};
    for (final row in songRows) {
      final playlistId = (row['playlistId'] ?? '').toString();
      final songId = (row['songId'] ?? '').toString();
      if (playlistId.isEmpty || songId.isEmpty) continue;
      songsMap.putIfAbsent(playlistId, () => []).add(songId);
    }
    return rows
        .map(
          (row) => PlaylistEntity(
            id: (row['id'] ?? '').toString(),
            name: (row['name'] ?? '').toString(),
            songIds: songsMap[(row['id'] ?? '').toString()] ?? const [],
            createdAtMs: row['createdAtMs'] is int
                ? row['createdAtMs'] as int
                : int.tryParse((row['createdAtMs'] ?? '').toString()) ?? 0,
          ),
        )
        .where((p) => p.id.isNotEmpty && p.name.trim().isNotEmpty)
        .toList();
  }

  Future<PlaylistEntity> createPlaylist(String name) async {
    final trimmed = name.trim().isEmpty ? '新建歌单' : name.trim();
    final now = DateTime.now().millisecondsSinceEpoch;
    final playlist = PlaylistEntity(
      id: now.toString(),
      name: trimmed,
      songIds: const [],
      createdAtMs: now,
    );
    final db = await DbHelper.instance.database;
    final maxOrder = await _maxSortOrder(db);
    await db.insert(DbConstants.tablePlaylists, {
      'id': playlist.id,
      'name': playlist.name,
      'createdAtMs': playlist.createdAtMs,
      'sortOrder': maxOrder + 1,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
    _bumpCacheVersion();
    return playlist;
  }

  Future<void> renamePlaylist(String id, String name) async {
    final trimmed = name.trim();
    if (id.isEmpty || trimmed.isEmpty) return;
    final db = await DbHelper.instance.database;
    await db.update(
      DbConstants.tablePlaylists,
      {'name': trimmed},
      where: 'id = ?',
      whereArgs: [id],
    );
    _bumpCacheVersion();
  }

  Future<void> deletePlaylist(String id) async {
    if (id.isEmpty) return;
    final db = await DbHelper.instance.database;
    await db.transaction((txn) async {
      await txn.delete(
        DbConstants.tablePlaylistSongs,
        where: 'playlistId = ?',
        whereArgs: [id],
      );
      await txn.delete(
        DbConstants.tablePlaylists,
        where: 'id = ?',
        whereArgs: [id],
      );
    });
    _bumpCacheVersion();
  }

  Future<void> addSongs(String playlistId, List<String> songIds) async {
    if (playlistId.isEmpty || songIds.isEmpty) return;
    final toAdd = songIds.where((e) => e.trim().isNotEmpty).toList();
    if (toAdd.isEmpty) return;
    final db = await DbHelper.instance.database;
    await db.transaction((txn) async {
      final existingRows = await txn.query(
        DbConstants.tablePlaylistSongs,
        columns: ['songId'],
        where: 'playlistId = ?',
        whereArgs: [playlistId],
      );
      final existing = existingRows
          .map((e) => (e['songId'] ?? '').toString())
          .toSet();
      final maxOrder = await _maxSongOrder(txn, playlistId);
      var nextOrder = maxOrder + 1;
      for (final id in toAdd) {
        if (existing.contains(id)) continue;
        await txn.insert(
          DbConstants.tablePlaylistSongs,
          {'playlistId': playlistId, 'songId': id, 'sortOrder': nextOrder},
          conflictAlgorithm: ConflictAlgorithm.ignore,
        );
        nextOrder += 1;
      }
    });
    _bumpCacheVersion();
  }

  Future<void> removeSongs(String playlistId, List<String> songIds) async {
    if (playlistId.isEmpty || songIds.isEmpty) return;
    final toRemove = songIds.where((e) => e.trim().isNotEmpty).toSet();
    if (toRemove.isEmpty) return;
    final db = await DbHelper.instance.database;
    final placeholders = List.filled(toRemove.length, '?').join(',');
    await db.delete(
      DbConstants.tablePlaylistSongs,
      where: 'playlistId = ? AND songId IN ($placeholders)',
      whereArgs: [playlistId, ...toRemove],
    );
    _bumpCacheVersion();
  }

  Future<void> reorderPlaylists(List<String> orderedIds) async {
    if (orderedIds.isEmpty) return;
    final db = await DbHelper.instance.database;
    await db.transaction((txn) async {
      final allRows = await txn.query(
        DbConstants.tablePlaylists,
        orderBy: 'sortOrder ASC',
      );
      final remaining = allRows
          .map((e) => (e['id'] ?? '').toString())
          .where((id) => id.isNotEmpty && !orderedIds.contains(id))
          .toList();
      final nextIds = [...orderedIds, ...remaining];
      var order = 1;
      for (final id in nextIds) {
        await txn.update(
          DbConstants.tablePlaylists,
          {'sortOrder': order},
          where: 'id = ?',
          whereArgs: [id],
        );
        order += 1;
      }
    });
    _bumpCacheVersion();
  }

  Future<void> movePlaylistToTop(String playlistId) async {
    if (playlistId.isEmpty) return;
    final db = await DbHelper.instance.database;
    await db.transaction((txn) async {
      final rows = await txn.query(
        DbConstants.tablePlaylists,
        orderBy: 'sortOrder ASC',
      );
      final ids = rows
          .map((e) => (e['id'] ?? '').toString())
          .where((id) => id.isNotEmpty)
          .toList();
      if (!ids.contains(playlistId)) return;
      ids.remove(playlistId);
      final nextIds = [playlistId, ...ids];
      var order = 1;
      for (final id in nextIds) {
        await txn.update(
          DbConstants.tablePlaylists,
          {'sortOrder': order},
          where: 'id = ?',
          whereArgs: [id],
        );
        order += 1;
      }
    });
    _bumpCacheVersion();
  }

  Future<void> reorderSongs(
    String playlistId,
    List<String> orderedSongIds,
  ) async {
    if (playlistId.isEmpty) return;
    final ids = orderedSongIds
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
    final db = await DbHelper.instance.database;
    await db.transaction((txn) async {
      final rows = await txn.query(
        DbConstants.tablePlaylistSongs,
        columns: ['songId'],
        where: 'playlistId = ?',
        whereArgs: [playlistId],
        orderBy: 'sortOrder ASC',
      );
      final existing = rows.map((e) => (e['songId'] ?? '').toString()).toList();
      final next = <String>[];
      for (final id in ids) {
        if (!existing.contains(id)) continue;
        if (next.contains(id)) continue;
        next.add(id);
      }
      for (final id in existing) {
        if (next.contains(id)) continue;
        next.add(id);
      }
      var order = 1;
      for (final id in next) {
        await txn.update(
          DbConstants.tablePlaylistSongs,
          {'sortOrder': order},
          where: 'playlistId = ? AND songId = ?',
          whereArgs: [playlistId, id],
        );
        order += 1;
      }
    });
    _bumpCacheVersion();
  }

  Future<int> _maxSortOrder(DatabaseExecutor executor) async {
    final rows = await executor.rawQuery(
      'SELECT MAX(sortOrder) as maxOrder FROM ${DbConstants.tablePlaylists}',
    );
    if (rows.isEmpty) return 0;
    final value = rows.first['maxOrder'];
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }

  Future<int> _maxSongOrder(
    DatabaseExecutor executor,
    String playlistId,
  ) async {
    final rows = await executor.rawQuery(
      'SELECT MAX(sortOrder) as maxOrder FROM ${DbConstants.tablePlaylistSongs} WHERE playlistId = ?',
      [playlistId],
    );
    if (rows.isEmpty) return 0;
    final value = rows.first['maxOrder'];
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }

  Future<bool> _migrateFromPrefs(Database db) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_prefsKey);
    if (raw == null || raw.trim().isEmpty) return false;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List) return false;
      final playlists = decoded
          .whereType<Map>()
          .map((e) => e.map((k, v) => MapEntry(k.toString(), v)))
          .map(PlaylistEntity.fromJson)
          .where((p) => p.id.isNotEmpty && p.name.trim().isNotEmpty)
          .toList();
      if (playlists.isEmpty) return false;
      await db.transaction((txn) async {
        var order = 0;
        for (final playlist in playlists) {
          await txn.insert(
            DbConstants.tablePlaylists,
            {
              'id': playlist.id,
              'name': playlist.name,
              'createdAtMs': playlist.createdAtMs,
              'sortOrder': order,
            },
            conflictAlgorithm: ConflictAlgorithm.ignore,
          );
          var songOrder = 1;
          for (final songId in playlist.songIds) {
            if (songId.trim().isEmpty) continue;
            await txn.insert(
              DbConstants.tablePlaylistSongs,
              {'playlistId': playlist.id, 'songId': songId, 'sortOrder': songOrder},
              conflictAlgorithm: ConflictAlgorithm.ignore,
            );
            songOrder += 1;
          }
          order += 1;
        }
      });
      await prefs.remove(_prefsKey);
      return true;
    } catch (_) {
      return false;
    }
  }
}
