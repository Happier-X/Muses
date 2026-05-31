import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/state/settings_state.dart';
import 'base/app_page_scaffold.dart';
import '../player/mini_player/mini_player_bar.dart';
import 'side_menu.dart';

class TabletLayoutHost extends StatefulWidget {
  final Widget child;
  final GlobalKey<NavigatorState> navigatorKey;

  const TabletLayoutHost({
    super.key,
    required this.child,
    required this.navigatorKey,
  });

  @override
  State<TabletLayoutHost> createState() => _TabletLayoutHostState();
}

class _TabletLayoutHostState extends State<TabletLayoutHost>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 260),
  );

  @override
  void initState() {
    super.initState();
    if (AppLayoutSettings.tabletMode.value) {
      _controller.value = 1;
    }
    AppLayoutSettings.tabletMode.addListener(_handleModeChanged);
  }

  @override
  void dispose() {
    AppLayoutSettings.tabletMode.removeListener(_handleModeChanged);
    _controller.dispose();
    super.dispose();
  }

  void _handleModeChanged() {
    if (!mounted) return;
    final enabled = AppLayoutSettings.tabletMode.value;
    if (enabled) {
      _controller.forward();
    } else {
      _controller.reverse();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final t = _controller.value;
        final width = MediaQuery.sizeOf(context).width;
        final drawerWidth = (width * 0.32).clamp(200.0, 260.0);
        final pageOffset = drawerWidth * t;
        final bottomInset = MediaQuery.paddingOf(context).bottom;
        final isTabletMode = AppLayoutSettings.tabletMode.value && t == 1;

        return PopScope(
          canPop: false,
          onPopInvokedWithResult: (didPop, result) {
            if (didPop) return;
            final navigator = widget.navigatorKey.currentState;
            if (navigator?.canPop() ?? false) {
              navigator?.pop();
              return;
            }
            SystemNavigator.pop();
          },
          child: Stack(
            children: [
              Positioned.fill(left: pageOffset, child: child!),
              Positioned(
                left: -drawerWidth + drawerWidth * t,
                top: 0,
                bottom: 0,
                width: drawerWidth,
                child: IgnorePointer(
                  ignoring: t == 0,
                  child: SideMenu(onNavigate: _handleNavigate),
                ),
              ),
              if (isTabletMode)
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: bottomInset,
                  child: MiniPlayerBar(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 4,
                    ),
                  ),
                ),
              ValueListenableBuilder<MiniPlayerOverlayConfig>(
                valueListenable: MiniPlayerOverlayState.config,
                builder: (context, config, _) {
                  if (!config.visible || AppLayoutSettings.tabletMode.value) {
                    return const SizedBox.shrink();
                  }
                  return Positioned(
                    left: 0,
                    right: 0,
                    bottom: config.hasBottomNav
                        ? bottomInset + AppPageScaffold.modernNavHeight
                        : bottomInset,
                    child: MiniPlayerBar(
                      padding: config.hasBottomNav
                          ? const EdgeInsets.fromLTRB(16, 4, 16, 0)
                          : const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 4,
                            ),
                    ),
                  );
                },
              ),
            ],
          ),
        );
      },
      child: widget.child,
    );
  }

  void _handleNavigate(String route) {
    widget.navigatorKey.currentState?.pushNamedAndRemoveUntil(
      route,
      (route) => false,
    );
  }
}
