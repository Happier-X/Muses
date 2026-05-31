# GitHub Action 构建速度优化 - 技术设计

## 1. 架构概述

优化目标：在保持构建所有 3 个 ABI（arm64-v8a、armeabi-v7a、x86_64）的前提下，将构建时间从七八分钟优化到2分钟以内。

## 2. 优化策略

### 2.1 Flutter 编译优化

**当前命令**：
```bash
flutter build apk --release --split-per-abi --flavor prod
```

**优化方案**：
1. **添加 `--no-tree-shake-icons`**：
   - 跳过图标 tree shaking 步骤
   - 可节省 30-60 秒编译时间
   - 副作用：APK 体积可能略增（包含未使用的图标）

2. **检查其他编译参数**：
   - `--split-debug-info`：分离调试信息（减小 APK 体积，但可能不影响编译速度）
   - `--obfuscate`：代码混淆（可能增加编译时间，不推荐）

### 2.2 Gradle 构建优化

**当前配置**（`android/gradle.properties`）：
```properties
org.gradle.jvmargs=-Xmx8G -XX:MaxMetaspaceSize=4G -XX:ReservedCodeCacheSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

**优化方案**：
1. **调整 JVM 内存配置**：
   - 当前：`-Xmx8G`（8GB 最大堆内存）
   - 建议：保持或适当增加，取决于运行器资源

2. **启用 Gradle Build Cache**：
   - 确保 `org.gradle.caching=true` 已启用
   - 考虑使用远程 Build Cache（如果有多次构建）

3. **优化 Gradle Daemon**：
   - 确保 `org.gradle.daemon=true` 已启用
   - 考虑调整 daemon 内存配置

### 2.3 运行器优化

**当前运行器**：`ubuntu-latest`

**优化方案**：
1. **使用更快的运行器**：
   - `ubuntu-latest-xl`：更多 CPU 和内存资源
   - 成本：可能增加 GitHub Actions 费用

2. **自托管运行器**：
   - 如果有自托管运行器资源，可以考虑使用
   - 优点：可定制硬件配置
   - 缺点：需要维护

### 2.4 缓存策略优化

**当前缓存**：
1. Flutter SDK 缓存（`subosito/flutter-action@v2` 的 `cache: true`）
2. Gradle 缓存（`gradle/actions/setup-gradle@v4`）

**优化方案**：
1. **添加 Pub 依赖缓存**：
   - 使用 `actions/cache` 缓存 `~/.pub-cache`
   - 减少 `flutter pub get` 时间

2. **精细化 Gradle 缓存**：
   - 缓存 `~/.gradle/caches`
   - 缓存 `~/.gradle/wrapper`

### 2.5 构建流程优化

**当前流程**：
1. 检出代码
2. 设置 Flutter SDK
3. 设置 Java
4. 设置 Gradle
5. `flutter pub get`
6. 配置签名
7. `flutter build apk`
8. 提取版本号
9. 重命名 APK
10. 提取更新日志
11. 创建 Release

**优化方案**：
1. **并行化**：
   - 检出代码和设置工具可以并行
   - 但 GitHub Actions 步骤是顺序执行的

2. **减少步骤**：
   - 合并相关步骤
   - 减少不必要的文件操作

## 3. 实施计划

### 3.1 第一阶段：快速优化（预计节省 1-2 分钟）

1. **添加 `--no-tree-shake-icons`**：
   - 修改构建命令
   - 预计节省：30-60 秒

2. **优化 Gradle 配置**：
   - 调整 JVM 参数
   - 确保缓存配置正确
   - 预计节省：30-60 秒

### 3.2 第二阶段：深度优化（预计节省 2-3 分钟）

1. **使用更快的运行器**：
   - 更改 `runs-on` 为 `ubuntu-latest-xl`
   - 预计节省：1-2 分钟

2. **精细化缓存**：
   - 添加 Pub 依赖缓存
   - 优化 Gradle 缓存策略
   - 预计节省：30-60 秒

### 3.3 第三阶段：流程优化（预计节省 1 分钟）

1. **合并步骤**：
   - 将相关步骤合并
   - 减少文件操作

2. **优化版本提取和重命名**：
   - 简化脚本
   - 减少不必要的命令

## 4. 风险评估

### 4.1 兼容性风险
- **风险**：优化可能影响构建产物的功能
- **缓解**：保持所有 ABI 构建，不修改应用代码逻辑

### 4.2 稳定性风险
- **风险**：优化可能导致构建失败
- **缓解**：逐步实施优化，每步验证构建结果

### 4.3 维护性风险
- **风险**：优化可能增加维护复杂度
- **缓解**：保持配置简洁，添加必要注释

## 5. 验证方案

### 5.1 构建时间验证
- **目标**：构建时间 ≤ 2 分钟
- **方法**：多次运行构建，取平均时间

### 5.2 构建产物验证
- **目标**：构建产物功能正常
- **方法**：
  1. 检查 APK 文件是否生成
  2. 验证 APK 文件大小合理
  3. 安装测试（可选）

### 5.3 兼容性验证
- **目标**：支持所有目标设备
- **方法**：
  1. 检查 ABI 文件是否完整
  2. 在不同架构设备上测试（可选）

## 6. 回滚方案

如果优化导致问题，可以快速回滚：
1. **Git 回滚**：恢复到优化前的配置
2. **手动触发**：使用 `workflow_dispatch` 手动触发构建
3. **逐步回滚**：逐个移除优化措施，定位问题