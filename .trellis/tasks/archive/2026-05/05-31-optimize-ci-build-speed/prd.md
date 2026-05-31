# 优化 GitHub Action 构建速度

## Goal

将 GitHub Action 构建时间从目前的七八分钟优化到更短，提高发版效率。

## 用户请求

用户希望优化发版时 GitHub Action 的构建速度，目前发布一个新版本需要七八分钟，时间太长。

## 已知事实

1. **当前工作流配置**：`.github/workflows/build-release.yml`
   - 触发条件：推送 `v*` 标签或手动触发
   - 运行环境：`ubuntu-latest`
   - 主要步骤：
     - 检出代码（带子模块，深度为1）
     - 设置 Flutter SDK（稳定版，启用缓存）
     - 设置 Java 17（Temurin 发行版）
     - 设置 Gradle（使用 `gradle/actions/setup-gradle@v4`）
     - 运行 `flutter pub get`
     - 配置签名
     - 运行 `flutter build apk --release --split-per-abi --flavor prod`
     - 提取版本号
     - 重命名 APK 文件
     - 提取更新日志
     - 创建 GitHub Release

2. **项目配置**：
   - Flutter 项目，Dart SDK `^3.10.8`
   - Android 目标，使用 Kotlin DSL 构建脚本
   - 有多个依赖，包括本地插件 `flutter_lyric`
   - 使用 `--flavor prod` 构建生产版本
   - 生成三个 ABI 的 APK：arm64-v8a、armeabi-v7a、x86_64

3. **当前优化措施**：
   - Flutter SDK 缓存已启用（`cache: true`）
   - Gradle 缓存已启用（`gradle/actions/setup-gradle@v4`）
   - 代码检出深度为1（`fetch-depth: 1`）

## 已知瓶颈

**主要瓶颈**：`flutter build apk --release --split-per-abi --flavor prod` 这一步骤

## 优化方向

### 1. 减少 ABI 构建数量
- 当前构建 3 个 ABI：arm64-v8a、armeabi-v7a、x86_64
- **方案 A**：只构建 arm64-v8a（覆盖 90%+ 现代设备）
- **方案 B**：构建 arm64-v8a + armeabi-v7a（兼容旧设备）

### 2. Flutter 编译优化参数
- `--no-tree-shake-icons`：跳过图标 tree shaking（可节省时间）
- `--split-debug-info`：分离调试信息（减小 APK 体积，但可能不影响编译速度）
- `--obfuscate`：代码混淆（可能增加编译时间，不推荐）

### 3. Gradle 构建优化
- 当前已启用：`org.gradle.parallel=true`、`org.gradle.caching=true`
- 可调整：JVM 内存配置、Gradle daemon 配置

### 4. 缓存策略优化
- Flutter SDK 缓存：已启用
- Gradle 依赖缓存：已启用
- Pub 依赖缓存：可能需要额外配置

## 范围

- 优化目标：将构建时间减少到合理范围（例如 5 分钟以内）
- 约束条件：不能影响构建产物的质量和功能
- 不在范围内：修改应用代码逻辑，只优化 CI/CD 流程

## 用户决策

1. **ABI 构建策略**：保持构建所有 3 个 ABI（arm64-v8a、armeabi-v7a、x86_64），兼容性优先
2. **优化目标**：在保持所有 ABI 的前提下，将构建时间从七八分钟优化到2分钟以内

## 优化策略（需保持所有ABI）

### 主要优化方向

1. **Flutter 编译参数优化**
   - `--no-tree-shake-icons`：跳过图标 tree shaking
   - 检查是否有其他可跳过的优化步骤

2. **Gradle 构建优化**
   - 调整 JVM 内存配置
   - 优化 Gradle daemon 设置
   - 启用增量编译

3. **运行器性能**
   - 考虑使用更快的 GitHub Actions 运行器（如 `ubuntu-latest-xl`）
   - 增加运行器资源

4. **缓存策略**
   - 确保 Flutter SDK、Gradle、Pub 缓存有效
   - 考虑使用 actions/cache 进行更精细的缓存控制

5. **构建流程优化**
   - 并行执行不相关的步骤
   - 减少不必要的步骤

## 实施计划总结

### 第一阶段：快速优化（预计节省 1-2 分钟）
1. 添加 `--no-tree-shake-icons` 编译参数
2. 优化 Gradle JVM 内存配置

### 第二阶段：深度优化（预计节省 2-3 分钟）
1. 使用 `ubuntu-latest-xl` 运行器
2. 添加 Pub 依赖缓存
3. 优化 Gradle 缓存策略

### 第三阶段：流程优化（预计节省 1 分钟）
1. 合并相关构建步骤
2. 优化版本提取和重命名脚本

### 预期效果
- **优化前**：7-8 分钟
- **优化后**：2 分钟以内
- **总节省**：5-6 分钟

## 验收标准

- [ ] 构建时间从目前的七八分钟减少到2分钟以内
- [ ] 构建产物功能不受影响（至少支持 arm64-v8a ABI）
- [ ] 优化措施可维护，不会增加未来维护成本

## 待解决问题

- 需要分析当前构建时间的具体分布
- 需要确定优化目标和优先级
