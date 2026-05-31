# 平板模式修复执行计划

## 实现步骤

### 步骤 1：修改默认值逻辑
**文件**：`lib/app/state/settings_layout_state.dart`

**改动**：
1. 添加 `_hasUserSet` 标志，记录用户是否手动设置过
2. 修改 `ensureLoaded()` 方法，如果用户没有手动设置过，默认值为 `false`
3. 添加 `setTabletMode()` 方法时，设置 `_hasUserSet = true`

**验证**：
- 检查 `SharedPreferences` 中是否有 `setting_tablet_mode` 键
- 如果没有，默认值应为 `false`

### 步骤 2：在 TabletLayoutHost 中根据屏幕宽度设置默认值
**文件**：`lib/components/layout/tablet_layout_host.dart`

**改动**：
1. 在 `initState()` 中，检查 `_hasUserSet` 标志
2. 如果用户没有手动设置过，根据屏幕宽度判断：
   - 如果宽度 > 600dp，设置 `AppLayoutSettings.tabletMode.value = true`，`_controller.value = 1`
   - 否则，保持默认值 `false`
3. 如果用户手动设置过，使用用户设置的值

**验证**：
- 在平板设备（宽度 > 600dp）上首次启动，应自动开启平板模式
- 在手机设备（宽度 <= 600dp）上首次启动，应保持关闭平板模式

### 步骤 3：修复白屏问题
**文件**：`lib/components/layout/tablet_layout_host.dart`

**改动**：
1. 修改 `_handleModeChanged()` 方法：
   - 当 `enabled` 为 `true` 时，先设置 `_controller.value = 0`，再调用 `_controller.forward()`
   - 确保动画开始时布局与手机模式一致
2. 修改 `MiniPlayerBar` 的显示条件：
   - 将 `if (AppLayoutSettings.tabletMode.value)` 改为 `if (t > 0)`
   - 确保 `MiniPlayerBar` 在动画期间不会立即显示

**验证**：
- 开启平板模式时，不应出现白屏
- 侧边栏应平滑展开，无闪烁
- `MiniPlayerBar` 应在侧边栏展开后显示

### 步骤 4：测试场景
**测试用例**：

1. **平板设备首次启动**
   - 预期：自动开启平板模式，侧边栏常开

2. **手机设备首次启动**
   - 预期：保持关闭平板模式，使用抽屉导航

3. **手动切换平板模式**
   - 预期：平滑动画，无白屏

4. **快速切换平板模式**
   - 预期：无动画异常或白屏

5. **小屏幕平板（宽度接近 600dp）**
   - 预期：正常显示，无布局溢出

## 验证命令

```bash
# 静态分析
flutter analyze

# 运行测试（如果有）
flutter test

# 在平板模拟器上测试
flutter run -d <tablet_device_id>

# 在手机模拟器上测试
flutter run -d <phone_device_id>
```

## 风险点

1. **屏幕宽度阈值**：600dp 是 Material Design 的标准阈值，但某些设备可能需要调整
2. **动画时序**：需要确保 `_controller.value = 0` 在 `_controller.forward()` 之前执行
3. **状态同步**：需要确保 `AppLayoutSettings.tabletMode` 和 `_controller.value` 始终同步

## 回滚点

如果出现问题，可以回滚以下文件：
- `lib/app/state/settings_layout_state.dart`
- `lib/components/layout/tablet_layout_host.dart`
