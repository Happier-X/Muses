import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppLayoutSettings {
  static const String _prefsTabletMode = 'setting_tablet_mode';
  static const String _prefsUserSetTabletMode = 'setting_user_set_tablet_mode';

  static final ValueNotifier<bool> tabletMode = ValueNotifier(false);

  static bool _loaded = false;
  static bool _hasUserSet = false;

  static bool get hasUserSet => _hasUserSet;

  static Future<void> ensureLoaded() async {
    if (_loaded) return;
    _loaded = true;
    final prefs = await SharedPreferences.getInstance();
    _hasUserSet = prefs.getBool(_prefsUserSetTabletMode) ?? false;
    if (_hasUserSet) {
      tabletMode.value = prefs.getBool(_prefsTabletMode) ?? false;
    } else {
      tabletMode.value = false;
    }
  }

  static Future<void> setTabletMode(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsTabletMode, enabled);
    await prefs.setBool(_prefsUserSetTabletMode, true);
    _hasUserSet = true;
    tabletMode.value = enabled;
  }
}
