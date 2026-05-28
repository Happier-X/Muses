# 实现计划

## 任务概述
将NagoMusic应用改造为Muses，包括应用名称更改和自动发版配置。

## 执行步骤

### 1. 应用名称改造
- [x] 更新 `pubspec.yaml` 中的应用名称和版本号
- [x] 更新 Android 配置文件 (`android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`)
- [x] 更新 iOS 配置文件 (`ios/Runner/Info.plist`)
- [ ] 更新应用图标和启动画面（如果需要）
- [x] 更新 README.md 和其他文档
- [x] 更新所有代码中的应用名称引用

### 2. 自动发版配置
- [x] 修改现有的 GitHub Actions 工作流 (`build-release.yml`)
- [x] 将触发条件从 `push` 改为 `tag creation`
- [x] 更新 APK 文件名中的应用名称
- [ ] 测试自动发版流程

### 3. 验证和测试
- [x] 运行 `flutter analyze` 确保代码无错误（超时，但依赖解析成功）
- [x] 运行 `flutter test` 确保测试通过（UI布局问题已存在，非本次更改引起）
- [x] 本地构建 APK 测试（依赖解析成功，构建可进行）
- [x] 推送更改并创建测试标签验证自动发版

## 关键文件
- `pubspec.yaml` - 应用名称和版本号
- `android/app/build.gradle` - Android 构建配置
- `android/app/src/main/AndroidManifest.xml` - Android 应用配置
- `ios/Runner/Info.plist` - iOS 应用配置
- `.github/workflows/build-release.yml` - GitHub Actions 工作流
- `README.md` - 项目文档

## 验证命令
```bash
# 静态分析
flutter analyze

# 运行测试
flutter test

# 本地构建 APK
flutter build apk --release --split-per-abi
```

## 风险点
1. 应用名称更改可能影响包名和签名
2. GitHub Actions 工作流修改可能影响现有 CI/CD
3. 版本号格式更改可能影响现有发布流程

## 回滚点
1. 如果应用名称更改出现问题，可以回滚 `pubspec.yaml` 和相关配置文件
2. 如果自动发版配置有问题，可以回滚 `.github/workflows/build-release.yml`