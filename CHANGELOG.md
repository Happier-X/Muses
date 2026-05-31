# 更新日志

所有版本的更新记录都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

## [0.0.8] - 2026-05-31

### 新增
- 平板模式支持，侧边栏常开显示
- WebDAV 添加时支持选择扫描文件夹

### 修复
- 修复平板模式下的白屏问题
- 修复歌曲页面标题居中问题
- 平板模式下去掉所有页面的汉堡图标
- 修复应用内更新签名不匹配问题（key.properties 属性名修正）

### 优化
- 移除侧边栏顶部 SafeArea，延伸到状态栏下方
- 平板模式下主内容区域去除圆角和空隙
- 优化 GitHub Action 构建速度，预计从 7-8 分钟缩短至 2 分钟以内
  - 使用 ubuntu-latest-xl 运行器
  - 添加 Gradle 和 Pub 依赖缓存
  - 添加 --no-tree-shake-icons 编译参数
  - 增加 Gradle JVM 内存至 12GB

## [0.0.7] - 2026-05-30

### 修复
- 修复歌曲页面按返回时回到歌曲页面而非退出应用的问题（移除重复路由）
- 修复 dialog/bottom sheet 被底部播放条遮挡的层级问题（useRootNavigator: true）
- 修复设置和统计页面的侧边栏导航行为，左上角显示汉堡菜单图标
- 侧边栏 logo 改用 app icon

## [0.0.6] - 2026-05-29

### 新增
- 实现 iOS 风格的丝滑页面过渡动画，当前页面和前一页一起滑动

### 优化
- 优化设置页面返回体验，消除卡顿感
- 预加载所有设置到 main.dart，避免页面重复加载
- 优化 app.dart 中的嵌套 ValueListenableBuilder，减少重建次数
- 异步化 AppBackground 的文件存在检查，避免阻塞主线程
- 移除 AppPageScaffold.build() 中的 _publishMiniPlayer() 调用，避免级联重建

## [0.0.5] - 2026-05-29

### 修复
- 修复底部播放栏随页面切换而移动的问题
- 将播放栏固定在应用底部，不再参与页面转场动画

### 优化
- 优化抽屉滑动响应性

## [0.0.4] - 2026-05-29

### 修复
- 修复下载完成后无法打开 APK 安装包的问题
- 添加 REQUEST_INSTALL_PACKAGES 权限
- 安装前检查并请求安装未知应用权限

## [0.0.3] - 2026-05-29

### 新增
- 应用内更新功能，支持检查新版本并下载安装
- 从 package_info_plus 读取版本号，移除硬编码
- 添加 dev/prod product flavors，调试版与正式版可共存
- 更新应用 Logo

### 优化
- 优化版本号显示，不包含构建号
- 修复 AppSettingTile 中的 const 问题

### 依赖更新
- signals: 6.3.0 → 7.0.0
- signals_flutter: 6.3.0 → 7.0.0
- file_picker: 10.3.10 → 12.0.0-beta.4
- flutter_local_notifications: 17.2.4 → 21.0.0
- package_info_plus: 8.3.1 → 10.1.0
- Android Gradle Plugin: 8.11.1 → 8.12.1

### 技术改进
- 修复 FilePicker API 迁移（platform → 静态方法）
- 移除 kotlin-android 插件，迁移到 Built-in Kotlin

## [0.0.2] - 2026-05-28

### 新增
- 应用正式更名为 Muses
- 配置自动化发布流程，支持标签触发自动构建和发布

### 优化
- 优化应用图标和启动画面
- 更新应用描述和文档

### 技术改进
- 升级 Flutter 到最新稳定版本
- 优化构建流程，提高编译效率
- 改进代码结构和可维护性

## [0.0.1] - 2026-05-28

### 新增
- 初始版本发布
- 基础音乐播放功能
- WebDAV 支持
- 本地音乐扫描和播放
