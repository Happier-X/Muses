import 'package:flutter/material.dart';

import '../../../app/state/settings_state.dart';
import 'app_background.dart';
import '../../player/mini_player/mini_player_bar.dart';
import '../modern_navigation_bar.dart';

class MiniPlayerOverlayConfig {
  final bool visible;
  final bool hasBottomNav;

  const MiniPlayerOverlayConfig({
    required this.visible,
    required this.hasBottomNav,
  });
}

class MiniPlayerOverlayState {
  static final ValueNotifier<MiniPlayerOverlayConfig> config = ValueNotifier(
    const MiniPlayerOverlayConfig(visible: true, hasBottomNav: false),
  );
}

class AppPageScaffold extends StatefulWidget {
  static const double modernNavHeight = 60.0;

  static double scrollableBottomPadding(
    BuildContext context, {
    bool hasBottomNav = false,
    bool showMiniPlayer = true,
    double minPadding = 24,
  }) {
    final bottomInset = MediaQuery.paddingOf(context).bottom;
    final miniPlayerPadding = showMiniPlayer
        ? MiniPlayerBar.estimatedHeight
        : 0.0;
    final bottomNavPadding = hasBottomNav ? modernNavHeight : 0.0;
    return bottomInset + miniPlayerPadding + bottomNavPadding + minPadding;
  }

  final PreferredSizeWidget? appBar;
  final Widget body;
  final bool extendBodyBehindAppBar;
  final bool useSafeArea;
  final bool resizeToAvoidBottomInset;
  final bool keepBottomOverlayFixed;
  final bool ignoreKeyboardInsets;
  final int? bottomNavIndex;
  final ValueChanged<int>? onBottomNavTap;
  final Widget? drawer;
  final bool showMiniPlayer;

  const AppPageScaffold({
    super.key,
    this.appBar,
    required this.body,
    this.extendBodyBehindAppBar = false,
    this.useSafeArea = true,
    this.resizeToAvoidBottomInset = false,
    this.keepBottomOverlayFixed = false,
    this.ignoreKeyboardInsets = false,
    this.bottomNavIndex,
    this.onBottomNavTap,
    this.drawer,
    this.showMiniPlayer = true,
  });

  @override
  State<AppPageScaffold> createState() => AppPageScaffoldState();
}

class AppPageScaffoldState extends State<AppPageScaffold>
    with SingleTickerProviderStateMixin {
  static const Duration _drawerDuration = Duration(milliseconds: 240);
  static const double _drawerSettleThreshold = 0.35;
  static const double _drawerFlingVelocity = 360.0;

  late final AnimationController _drawerController = AnimationController(
    vsync: this,
    duration: _drawerDuration,
  );
  bool _draggingDrawer = false;

  bool get _hasDrawer => widget.drawer != null;

  bool get _hasBottomNav =>
      widget.bottomNavIndex != null && widget.onBottomNavTap != null;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _publishMiniPlayer());
  }

  @override
  void didUpdateWidget(covariant AppPageScaffold oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.showMiniPlayer != widget.showMiniPlayer ||
        oldWidget.bottomNavIndex != widget.bottomNavIndex ||
        oldWidget.onBottomNavTap != widget.onBottomNavTap) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _publishMiniPlayer());
    }
  }

  void _publishMiniPlayer() {
    if (!mounted) return;
    final route = ModalRoute.of(context);
    if (route != null && !route.isCurrent) return;
    MiniPlayerOverlayState.config.value = MiniPlayerOverlayConfig(
      visible: widget.showMiniPlayer,
      hasBottomNav: _hasBottomNav,
    );
  }

  void openDrawer() {
    if (!_hasDrawer) return;
    if (AppLayoutSettings.tabletMode.value) return;
    _drawerController.forward();
  }

  void closeDrawer() {
    if (!_hasDrawer) return;
    if (AppLayoutSettings.tabletMode.value) return;
    _drawerController.reverse();
  }

  void _settleDrawer(DragEndDetails details) {
    final velocity = details.primaryVelocity ?? 0;
    if (velocity.abs() >= _drawerFlingVelocity) {
      if (velocity > 0) {
        openDrawer();
      } else {
        closeDrawer();
      }
      return;
    }

    if (_drawerController.value < _drawerSettleThreshold) {
      closeDrawer();
    } else {
      openDrawer();
    }
  }

  @override
  void dispose() {
    _drawerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    Widget content = widget.body;
    if (widget.useSafeArea) {
      content = SafeArea(child: content);
    }
    if (widget.ignoreKeyboardInsets) {
      final mq = MediaQuery.of(context);
      content = MediaQuery(
        data: mq.copyWith(viewInsets: EdgeInsets.zero),
        child: content,
      );
    }

    // moved to initState and didUpdateWidget

    final hasBottomNav = _hasBottomNav;
    final bottomBar = hasBottomNav
        ? ModernNavigationBar(
            currentIndex: widget.bottomNavIndex!,
            onTap: widget.onBottomNavTap!,
          )
        : null;
    final drawerWidth = (MediaQuery.sizeOf(context).width * 0.62).clamp(
      220.0,
      300.0,
    );

    return ValueListenableBuilder<bool>(
      valueListenable: AppLayoutSettings.tabletMode,
      builder: (context, tabletMode, _) {
        Widget page = Scaffold(
          resizeToAvoidBottomInset: widget.resizeToAvoidBottomInset,
          extendBody: bottomBar != null,
          extendBodyBehindAppBar: widget.extendBodyBehindAppBar,
          backgroundColor: Colors.transparent,
          appBar: widget.appBar,
          body: content,
          bottomNavigationBar: bottomBar == null
              ? null
              : Material(type: MaterialType.transparency, child: bottomBar),
        );

        if (tabletMode || !_hasDrawer) {
          return AppBackground(child: page);
        }
        final stack = AppBackground(
          child: Stack(
            children: [
              AnimatedBuilder(
                animation: _drawerController,
                builder: (context, child) {
                  final value = _drawerController.value;
                  return Transform.translate(
                    offset: Offset(-drawerWidth + drawerWidth * value, 0),
                    child: child,
                  );
                },
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: SizedBox(width: drawerWidth, child: widget.drawer),
                ),
              ),
              AnimatedBuilder(
                animation: _drawerController,
                builder: (context, child) {
                  final value = _drawerController.value;
                  return GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onHorizontalDragStart: (_) {
                      _draggingDrawer = true;
                    },
                    onHorizontalDragUpdate: (details) {
                      if (!_draggingDrawer) return;
                      final delta = details.primaryDelta ?? 0;
                      if (delta == 0) return;
                      if (_drawerController.value == 0 && delta < 0) return;
                      if (_drawerController.value == 1 && delta > 0) return;
                      final next =
                          (_drawerController.value + delta / drawerWidth).clamp(
                            0.0,
                            1.0,
                          );
                      _drawerController.value = next;
                    },
                    onHorizontalDragEnd: (details) {
                      if (!_draggingDrawer) return;
                      _draggingDrawer = false;
                      _settleDrawer(details);
                    },
                    onHorizontalDragCancel: () {
                      if (!_draggingDrawer) return;
                      _draggingDrawer = false;
                      if (_drawerController.value < _drawerSettleThreshold) {
                        closeDrawer();
                      } else {
                        openDrawer();
                      }
                    },
                    child: Transform.translate(
                      offset: Offset(drawerWidth * value, 0),
                      child: child,
                    ),
                  );
                },
                child: page,
              ),
              AnimatedBuilder(
                animation: _drawerController,
                builder: (context, child) {
                  if (_drawerController.value == 0) {
                    return const SizedBox.shrink();
                  }
                  return Positioned(
                    left: drawerWidth,
                    top: 0,
                    right: 0,
                    bottom: 0,
                    child: GestureDetector(
                      onTap: closeDrawer,
                      onHorizontalDragUpdate: (details) {
                        final delta = details.primaryDelta ?? 0;
                        if (delta == 0) return;
                        final next =
                            (_drawerController.value + delta / drawerWidth)
                                .clamp(0.0, 1.0);
                        _drawerController.value = next;
                      },
                      onHorizontalDragEnd: (details) {
                        _settleDrawer(details);
                      },
                      child: Container(color: Colors.transparent),
                    ),
                  );
                },
              ),
            ],
          ),
        );
        return stack;
      },
    );
  }
}
