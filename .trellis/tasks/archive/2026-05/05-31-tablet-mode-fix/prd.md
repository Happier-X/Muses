# 修复平板模式白屏并默认开启

## Goal

修复平板模式下的白屏问题，并实现平板设备默认开启平板模式（侧边栏常开）。

## Confirmed Facts

### 架构
- 平板模式使用 `TabletLayoutHost`（`tablet_layout_host.dart`）管理侧边栏 + 内容区布局
- `AppPageScaffold`（`app_page_scaffold.dart`）管理手机模式下的抽屉（Drawer）
- 状态存储在 `AppLayoutSettings.tabletMode`（`settings_layout_state.dart`）

### 潜在白屏原因（按风险排序）
1. **动画中途状态不一致**（高风险）：`TabletLayoutHost` 使用动画驱动侧边栏展开，但 `AppPageScaffold` 基于布尔值二值切换，可能导致布局计算异常
2. **Navigator 路由栈被意外清空**（高风险）：侧边栏导航会清空 baseNavigator 的路由栈
3. **contentWidth 为 0 的边界情况**（中风险）：小屏幕设备上 contentWidth 可能过小
4. **双重 BackdropFilter 性能问题**（中风险）：平板模式下可能嵌套多层 BackdropFilter
5. **ClipRect + Transform.scale 约束传递**（中风险）：可能导致响应式布局计算错误

### 默认开启逻辑
- 当前默认值：`tabletMode.value = prefs.getBool(_prefsTabletMode) ?? false`（默认关闭）
- 需要改为根据设备类型自动判断

## Requirements

- 修复平板模式下的白屏问题
- 平板设备默认开启平板模式（侧边栏常开）
- 手机设备保持默认关闭平板模式
- 保留用户手动切换平板模式的能力
- 不影响现有手机模式的正常使用

## Acceptance Criteria

- [ ] 平板设备首次启动时自动开启平板模式
- [ ] 平板模式下侧边栏正常显示，无白屏
- [ ] 手机设备保持默认关闭平板模式
- [ ] 用户可在设置中手动切换平板模式
- [ ] 切换平板模式时无动画异常或白屏
- [ ] 所有页面在平板模式下正常显示

## Design Decisions

- 白屏发生在开启平板模式时
- 使用屏幕宽度判断设备类型（宽度 > 600dp 为平板）

## Open Questions

- 无

## Notes

- 这是一个复杂任务，已完成 design.md 和 implement.md
- 涉及多个文件的修改，需要仔细测试
