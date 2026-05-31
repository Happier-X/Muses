import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/state/settings_state.dart';
import 'base/app_page_scaffold.dart';
import 'base/app_background.dart';
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
    // 等待第一帧获取正确的屏幕宽度
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      if (!AppLayoutSettings.hasUserSet) {
        final width = MediaQuery.sizeOf(context).width;
        if (width > 600) {
          AppLayoutSettings.tabletMode.value = true;
          _controller.value = 1;
        }
      } else if (AppLayoutSettings.tabletMode.value) {
        _controller.value = 1;
      }
    });
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
      // 确保动画开始时布局与手机模式一致
      _controller.value = 0;
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
        final scale = 1 - (0.02 * t);
        final contentWidth = (width - pageOffset).clamp(0.0, width);
        final bottomInset = MediaQuery.paddingOf(context).bottom;
        final pageRadius = 24.0 * t;
        final pageShadow = Theme.of(context).brightness == Brightness.dark
            ? Colors.black.withValues(alpha: 0.28 * t)
            : Colors.black.withValues(alpha: 0.08 * t);

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
          child: AppBackground(
            child: Stack(
              children: [
                Positioned.fill(
                  child: ClipRect(
                    child: Padding(
                      padding: EdgeInsets.only(left: pageOffset),
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: SizedBox(
                          width: contentWidth,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(pageRadius),
                              boxShadow: [
                                BoxShadow(
                                  color: pageShadow,
                                  blurRadius: 28 * t,
                                  offset: Offset(0, 10 * t),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(pageRadius),
                              child: Transform.scale(
                                scale: scale,
                                alignment: Alignment.centerLeft,
                                child: child,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
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
                if (t > 0)
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
