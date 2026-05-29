import 'package:flutter/material.dart';
import 'package:flutter_displaymode/flutter_displaymode.dart';

import 'app/app.dart';
import 'app/services/debug_log_service.dart';
import 'app/state/settings_state.dart';
import 'app/services/media_notification_service.dart';
import 'app/services/db/dao/song_dao.dart';
import 'pages/player/widgets/player_background.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DebugLogService.instance.ensureLoaded();
  await FlutterDisplayMode.setHighRefreshRate();
  await MediaNotificationService.init();
  await AppThemeSettings.ensureLoaded();
  await AppLayoutSettings.ensureLoaded();
  await AppBackgroundSettings.ensureLoaded();
  await PlayerBackgroundSettings.ensureLoaded();
  await PlayerBottomActionSettings.ensureLoaded();
  await WebDavPlaybackSettings.ensureLoaded();
  await MediaNotificationSettings.ensureLoaded();
  await AppCacheSettings.ensureLoaded();
  await SongDownloadSettings.ensureLoaded();
  await LibraryRefreshSettings.ensureLoaded();
  await AppLaunchPlaybackSettings.ensureLoaded();
  await MiniPlayerInfoSettings.ensureLoaded();
  runApp(const MusesApp());
  SongDao().fetchAllCached();
}
