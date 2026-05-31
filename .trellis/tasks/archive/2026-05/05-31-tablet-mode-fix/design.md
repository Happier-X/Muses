# 平板模式修复技术设计

## 问题分析

### 白屏原因
开启平板模式时，存在以下时序问题：

1. `AppLayoutSettings.tabletMode.value` 立即变为 `true`
2. `AppPageScaffold` 立即停止渲染自己的 Drawer（基于 `tabletMode` 布尔值）
3. `TabletLayoutHost._handleModeChanged()` 触发 `_controller.forward()` 动画（260ms）
4. 在动画期间，`contentWidth` 从 `width` 逐渐减小到 `width - drawerWidth`
5. `Transform.scale` 从 1.0 逐渐减小到 0.98

**关键问题**：`AppPageScaffold` 的行为是二值切换（立即生效），但 `TabletLayoutHost` 使用动画驱动，导致布局计算不一致。

### 可能的白屏场景
1. **动画中途布局异常**：`contentWidth` 和 `scale` 同时变化，某些页面的响应式布局可能计算错误
2. **双重 BackdropFilter**：平板模式下 `AppBackground` 被嵌套多层
3. **MiniPlayerBar 立即显示**：基于 `tabletMode` 布尔值，不是基于动画值

## 解决方案

### 方案一：统一动画驱动（推荐）

让 `AppPageScaffold` 也使用动画值 `t` 来决定是否渲染 Drawer，而不是基于布尔值。

**优点**：
- 布局变化平滑，无突变
- 避免动画中途状态不一致

**缺点**：
- 需要修改 `AppPageScaffold` 的架构
- 需要将 `TabletLayoutHost` 的动画值传递给所有 `AppPageScaffold`

### 方案二：延迟切换（简单）

在 `AppPageScaffold` 中，当 `tabletMode` 变为 `true` 时，延迟到动画完成后再停止渲染 Drawer。

**优点**：
- 实现简单
- 不需要修改架构

**缺点**：
- 需要硬编码动画时长
- 可能存在微妙的时序问题

### 方案三：立即切换 + 动画平滑（折中）

保持 `AppPageScaffold` 的立即切换行为，但优化 `TabletLayoutHost` 的动画逻辑，确保在 `t=0` 时布局与手机模式完全一致。

**优点**：
- 不需要修改 `AppPageScaffold`
- 动画开始时布局无变化

**缺点**：
- 需要仔细调整动画参数

## 推荐方案：方案三

选择方案三，因为：
1. 改动范围最小
2. 不需要修改 `AppPageScaffold` 的架构
3. 可以通过调整 `TabletLayoutHost` 的动画逻辑来解决白屏问题

## 实现细节

### 1. 修改默认值逻辑

在 `settings_layout_state.dart` 中，根据屏幕宽度自动判断默认值：

```dart
static Future<void> ensureLoaded() async {
  if (_loaded) return;
  _loaded = true;
  final prefs = await SharedPreferences.getInstance();
  final savedValue = prefs.getBool(_prefsTabletMode);
  if (savedValue != null) {
    tabletMode.value = savedValue;
  } else {
    // 根据屏幕宽度判断，默认值在首次获取 MediaQuery 时设置
    tabletMode.value = false; // 默认关闭，后续在 TabletLayoutHost 中判断
  }
}
```

在 `TabletLayoutHost` 中，首次构建时根据屏幕宽度设置默认值：

```dart
@override
void initState() {
  super.initState();
  // 如果用户没有手动设置过，根据屏幕宽度判断
  if (!_hasUserSet) {
    final width = MediaQuery.sizeOf(context).width;
    if (width > 600) {
      AppLayoutSettings.tabletMode.value = true;
      _controller.value = 1;
    }
  } else if (AppLayoutSettings.tabletMode.value) {
    _controller.value = 1;
  }
  AppLayoutSettings.tabletMode.addListener(_handleModeChanged);
}
```

### 2. 修复白屏问题

在 `TabletLayoutHost` 中，确保动画开始时布局无变化：

```dart
void _handleModeChanged() {
  if (!mounted) return;
  final enabled = AppLayoutSettings.tabletMode.value;
  if (enabled) {
    // 立即设置 t=0，确保布局与手机模式一致
    _controller.value = 0;
    // 然后开始动画
    _controller.forward();
  } else {
    _controller.reverse();
  }
}
```

### 3. 优化 MiniPlayerBar 显示

将 `MiniPlayerBar` 的显示逻辑改为基于动画值 `t`：

```dart
if (t > 0) // 而不是 if (AppLayoutSettings.tabletMode.value)
  Positioned(
    left: 0,
    right: 0,
    bottom: bottomInset,
    child: MiniPlayerBar(...),
  ),
```

## 兼容性

- 不影响现有手机模式
- 保留用户手动切换平板模式的能力
- 首次启动时根据屏幕宽度自动判断，后续使用用户设置

## 回滚方案

如果出现问题，可以：
1. 恢复 `settings_layout_state.dart` 的默认值逻辑
2. 恢复 `TabletLayoutHost` 的动画逻辑
3. 恢复 `MiniPlayerBar` 的显示逻辑
