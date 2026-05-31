# GitHub Action 构建速度优化 - 执行计划

## 任务信息

- **任务路径**：`.trellis/tasks/05-31-optimize-ci-build-speed`
- **目标**：将构建时间从七八分钟优化到2分钟以内
- **约束**：保持构建所有 3 个 ABI（arm64-v8a、armeabi-v7a、x86_64）

## 执行步骤

### 第一阶段：快速优化（预计节省 1-2 分钟）

#### 步骤 1.1：添加 Flutter 编译优化参数
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  - run: flutter build apk --release --split-per-abi --flavor prod --no-tree-shake-icons
  ```
- **预期效果**：节省 30-60 秒
- **验证命令**：检查构建日志，确认 `--no-tree-shake-icons` 参数生效

#### 步骤 1.2：优化 Gradle 配置
- **文件**：`android/gradle.properties`
- **修改内容**：
  ```properties
  # 调整 JVM 内存配置（根据运行器资源调整）
  org.gradle.jvmargs=-Xmx12G -XX:MaxMetaspaceSize=4G -XX:ReservedCodeCacheSize=512m -XX:+HeapDumpOnOutOfMemoryError
  
  # 确保以下配置已启用
  org.gradle.daemon=true
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configureondemand=true
  ```
- **预期效果**：节省 30-60 秒
- **验证命令**：检查构建日志，确认 Gradle 配置生效

### 第二阶段：深度优化（预计节省 2-3 分钟）

#### 步骤 2.1：使用更快的运行器
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  jobs:
    build-android:
      runs-on: ubuntu-latest-xl  # 更多 CPU 和内存资源
  ```
- **预期效果**：节省 1-2 分钟
- **验证命令**：检查构建时间，确认运行器资源提升

#### 步骤 2.2：添加 Pub 依赖缓存
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  - uses: actions/cache@v4
    with:
      path: |
        ~/.pub-cache
        .dart_tool
      key: ${{ runner.os }}-pub-${{ hashFiles('**/pubspec.lock') }}
      restore-keys: |
        ${{ runner.os }}-pub-
  ```
- **预期效果**：节省 30-60 秒
- **验证命令**：检查缓存命中率，确认 Pub 依赖缓存生效

#### 步骤 2.3：优化 Gradle 缓存策略
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  - uses: actions/cache@v4
    with:
      path: |
        ~/.gradle/caches
        ~/.gradle/wrapper
      key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      restore-keys: |
        ${{ runner.os }}-gradle-
  ```
- **预期效果**：节省 30-60 秒
- **验证命令**：检查缓存命中率，确认 Gradle 缓存生效

### 第三阶段：流程优化（预计节省 1 分钟）

#### 步骤 3.1：合并相关步骤
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  - name: Build and prepare APKs
    run: |
      flutter build apk --release --split-per-abi --flavor prod --no-tree-shake-icons
      
      VERSION=$(grep '^version:' pubspec.yaml | awk '{print $2}' | cut -d'+' -f1)
      echo "VERSION=$VERSION" >> $GITHUB_ENV
      
      ls -la build/app/outputs/flutter-apk
      mv build/app/outputs/flutter-apk/*arm64-v8a*release.apk muses-v${VERSION}-arm64-v8a.apk
      mv build/app/outputs/flutter-apk/*armeabi-v7a*release.apk muses-v${VERSION}-armeabi-v7a.apk
      mv build/app/outputs/flutter-apk/*x86_64*release.apk muses-v${VERSION}-x86_64.apk
  ```
- **预期效果**：节省 30 秒
- **验证命令**：检查构建日志，确认步骤合并成功

#### 步骤 3.2：优化版本提取和重命名脚本
- **文件**：`.github/workflows/build-release.yml`
- **修改内容**：
  ```yaml
  - name: Extract version and rename APKs
    run: |
      VERSION=$(grep '^version:' pubspec.yaml | sed 's/version: //' | cut -d'+' -f1)
      echo "VERSION=$VERSION" >> $GITHUB_ENV
      
      cd build/app/outputs/flutter-apk
      for apk in *-release.apk; do
        abi=$(echo "$apk" | grep -oP '(arm64-v8a|armeabi-v7a|x86_64)')
        if [ -n "$abi" ]; then
          mv "$apk" "muses-v${VERSION}-${abi}.apk"
        fi
      done
  ```
- **预期效果**：节省 15 秒
- **验证命令**：检查 APK 文件名，确认重命名正确

## 验证清单

### 构建时间验证
- [ ] 运行优化后的构建，记录总时间
- [ ] 多次运行，取平均时间
- [ ] 确认构建时间 ≤ 2 分钟

### 构建产物验证
- [ ] 检查 APK 文件是否生成（3 个 ABI）
- [ ] 验证 APK 文件大小合理
- [ ] 检查 APK 文件名是否正确

### 兼容性验证
- [ ] 确认支持所有 3 个 ABI
- [ ] 检查构建日志，确认无错误

### 缓存验证
- [ ] 检查 Flutter SDK 缓存命中率
- [ ] 检查 Gradle 缓存命中率
- [ ] 检查 Pub 依赖缓存命中率

## 风险控制

### 风险 1：优化导致构建失败
- **缓解措施**：逐步实施优化，每步验证构建结果
- **回滚方案**：Git 回滚到优化前的配置

### 风险 2：优化影响构建产物功能
- **缓解措施**：保持所有 ABI 构建，不修改应用代码逻辑
- **验证方法**：检查 APK 文件是否完整，必要时进行安装测试

### 风险 3：优化增加维护复杂度
- **缓解措施**：保持配置简洁，添加必要注释
- **验证方法**：检查配置文件可读性

## 回滚点

1. **步骤 1.1 回滚**：移除 `--no-tree-shake-icons` 参数
2. **步骤 1.2 回滚**：恢复原始 Gradle 配置
3. **步骤 2.1 回滚**：恢复 `ubuntu-latest` 运行器
4. **步骤 2.2 回滚**：移除 Pub 依赖缓存
5. **步骤 2.3 回滚**：移除 Gradle 缓存优化
6. **步骤 3.1 回滚**：恢复原始步骤结构
7. **步骤 3.2 回滚**：恢复原始版本提取和重命名脚本

## 后续检查

### 构建后检查
1. **构建时间**：确认优化效果
2. **构建产物**：确认 APK 文件完整
3. **构建日志**：确认无错误和警告

### 长期监控
1. **构建时间趋势**：监控构建时间变化
2. **缓存命中率**：监控缓存效果
3. **构建失败率**：监控构建稳定性

## 相关文件

- `.github/workflows/build-release.yml`：GitHub Action 工作流配置
- `android/gradle.properties`：Gradle 构建配置
- `pubspec.yaml`：Flutter 项目配置
- `pubspec.lock`：依赖锁定文件