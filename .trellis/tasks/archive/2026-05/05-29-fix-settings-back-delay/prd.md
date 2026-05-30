# 优化设置页面返回体验

## Goal

消除从设置二级页面返回时的停顿感，提供流畅的返回体验。

## Requirements

### 功能需求
1. 从任意设置二级页面（如外观、播放器、通知等）返回设置主页时，不应有可感知的停顿
2. 返回动画应流畅，无卡顿
3. 保持现有页面跳转功能不变

### 技术约束
1. 不改变现有页面路由结构
2. 不影响设置页面的数据加载逻辑
3. 兼容现有的主题和玻璃效果系统

### 用户体验要求
1. 返回响应时间应 < 100ms（用户感知层面）
2. 动画流畅度应达到 60fps
3. 无白屏或闪烁现象

## Acceptance Criteria

- [ ] 从任意设置二级页面返回时无可感知停顿
- [ ] 返回动画流畅，无卡顿
- [ ] 所有设置页面功能正常
- [ ] 玻璃效果和背景效果正常工作
- [ ] 在不同设备上测试通过（高中低端）

## Technical Context

### 已识别的潜在瓶颈
1. **Widget树重建**：返回时父页面重建，触发所有ValueListenableBuilder
2. **BackdropFilter渲染**：玻璃效果启用时，GlassPanel组件需要重新渲染
3. **背景效果**：AppBackground带有发光效果和可选的图像模糊
4. **异步初始化**：ensureLoaded()可能还在等待中
5. **动画时长**：默认Material页面过渡时长（300ms）

### 相关文件
- 主设置页面：`lib/pages/settings/settings_page.dart`
- 二级页面：`lib/pages/settings/` 目录下的各个页面
- 状态管理：`lib/app/state/settings_*.dart`
- 页面路由：`lib/app/router/app_router.dart`
- 页面过渡动画：`lib/app/theme/app_styles.dart`
- 玻璃面板组件：`lib/components/common/glass_panel.dart`
- 背景组件：`lib/components/layout/base/app_background.dart`

## Optimization Plan

### 1. 移除重复的 ensureLoaded() 调用 ✅
- 将所有设置预加载到 `main.dart` 中
- 移除所有设置页面中的重复 `ensureLoaded()` 调用
- 避免了在每个页面中重复调用 `ensureLoaded()`

### 2. 优化 Widget 树重建 ✅
- 将 `GlassPanel` 中嵌套的 `ValueListenableBuilder` 替换为 `AnimatedBuilder`
- 减少了重建次数

### 3. 优化 BackdropFilter 渲染
- 缓存 `BackdropFilter` 的结果
- 减少模糊强度或禁用玻璃效果（如果性能问题严重）

### 4. 优化背景效果
- 减少发光效果的复杂度
- 缓存发光效果的结果

### 5. 优化页面过渡动画
- 调整动画曲线或时长（如果必要）

## Notes

- 这是一个轻量级优化任务，主要关注性能优化
- 需要实际测试验证优化效果
- 可能需要使用Flutter DevTools进行性能分析
