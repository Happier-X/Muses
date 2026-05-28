import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class MediaNotificationSettings {
  static const String _prefsShowLyrics = 'notification_show_lyrics';
  static const String _prefsShowCloseAction = 'notification_show_close_action';
  static const String _prefsLyricOnTop = 'notification_lyric_on_top';

  static final ValueNotifier<bool> showLyrics = ValueNotifier(true);
  static final ValueNotifier<bool> showCloseAction = ValueNotifier(true);
  static final ValueNotifier<bool> lyricOnTop = ValueNotifier(false);

  static bool _loaded = false;

  static Future<void> ensureLoaded() async {
    if (_loaded) return;
    _loaded = true;
    final prefs = await SharedPreferences.getInstance();
    showLyrics.value = prefs.getBool(_prefsShowLyrics) ?? true;
    showCloseAction.value = prefs.getBool(_prefsShowCloseAction) ?? true;
    lyricOnTop.value = prefs.getBool(_prefsLyricOnTop) ?? false;
  }

  static Future<void> setShowLyrics(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsShowLyrics, enabled);
    showLyrics.value = enabled;
  }

  static Future<void> setShowCloseAction(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsShowCloseAction, enabled);
    showCloseAction.value = enabled;
  }

  static Future<void> setLyricOnTop(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsLyricOnTop, enabled);
    lyricOnTop.value = enabled;
  }
}
