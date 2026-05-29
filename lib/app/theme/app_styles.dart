import 'package:flutter/material.dart';

import '../state/settings_background_state.dart';

class AppScrollBehavior extends MaterialScrollBehavior {
  const AppScrollBehavior();

  @override
  Widget buildOverscrollIndicator(
    BuildContext context,
    Widget child,
    ScrollableDetails details,
  ) {
    return child;
  }
}

class CoverPageTransitionsBuilder extends PageTransitionsBuilder {
  const CoverPageTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    return _CoverPageTransition(
      animation: animation,
      secondaryAnimation: secondaryAnimation,
      child: child,
    );
  }
}

class _CoverPageTransition extends StatefulWidget {
  final Animation<double> animation;
  final Animation<double> secondaryAnimation;
  final Widget child;

  const _CoverPageTransition({
    required this.animation,
    required this.secondaryAnimation,
    required this.child,
  });

  @override
  State<_CoverPageTransition> createState() => _CoverPageTransitionState();
}

class _CoverPageTransitionState extends State<_CoverPageTransition> {
  late final CurvedAnimation _primaryAnimation;
  late final CurvedAnimation _secondaryAnimation;

  @override
  void initState() {
    super.initState();
    _primaryAnimation = CurvedAnimation(
      parent: widget.animation,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeOutCubic,
    );
    _secondaryAnimation = CurvedAnimation(
      parent: widget.secondaryAnimation,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeOutCubic,
    );
  }

  @override
  void dispose() {
    _primaryAnimation.dispose();
    _secondaryAnimation.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // 当前页面：从右侧滑入（进入）或向右滑出（返回）
    final primaryOffset = Tween<Offset>(
      begin: const Offset(1.0, 0.0),
      end: Offset.zero,
    ).animate(_primaryAnimation);

    // 前一页：向左滑出（进入）或从左侧滑入（返回）
    final secondaryOffset = Tween<Offset>(
      begin: Offset.zero,
      end: const Offset(-0.3, 0.0),
    ).animate(_secondaryAnimation);

    return SlideTransition(
      position: secondaryOffset,
      child: SlideTransition(
        position: primaryOffset,
        child: widget.child,
      ),
    );
  }
}

extension AppThemeSurfaceX on ThemeData {
  bool get hasAmbientBackground {
    final backgroundPath = AppBackgroundSettings.backgroundImagePath.value;
    return AppBackgroundSettings.pageGlowEnabled.value ||
        (backgroundPath != null && backgroundPath.trim().isNotEmpty);
  }

  Color get appPanelColor {
    final isDark = brightness == Brightness.dark;
    final glassEnabled = AppBackgroundSettings.glassEffectEnabled.value;
    final panelOpacity = glassEnabled
        ? AppBackgroundSettings.panelOpacity.value
        : 1.0;
    if (panelOpacity <= 0) return Colors.transparent;
    final base = isDark
        ? Color.alphaBlend(
            colorScheme.primary.withValues(alpha: 0.08),
            colorScheme.surfaceContainerHigh,
          )
        : Color.alphaBlend(
            colorScheme.primary.withValues(alpha: 0.12),
            Colors.white,
          );
    if (!glassEnabled || !hasAmbientBackground) {
      return base.withValues(alpha: panelOpacity);
    }

    final overlayColor = isDark
        ? colorScheme.surfaceContainerHighest.withValues(alpha: panelOpacity)
        : Colors.white.withValues(alpha: panelOpacity);

    return Color.alphaBlend(
      colorScheme.primary.withValues(alpha: 0.06),
      overlayColor,
    );
  }

  Color get appPanelShadowColor {
    final isDark = brightness == Brightness.dark;
    return isDark
        ? Colors.black.withValues(alpha: 0.35)
        : colorScheme.primary.withValues(alpha: 0.16);
  }

  Color get appPanelBorderColor {
    final isDark = brightness == Brightness.dark;
    return isDark
        ? colorScheme.outline.withValues(alpha: 0.36)
        : colorScheme.primary.withValues(alpha: 0.12);
  }

  Color get appPanelElevatedColor {
    final base = appPanelColor;
    if (base.a <= 0) return Colors.transparent;
    final overlay = brightness == Brightness.dark
        ? Colors.white.withValues(alpha: 0.05)
        : Colors.white.withValues(alpha: 0.25);
    return Color.alphaBlend(overlay, base);
  }
}
