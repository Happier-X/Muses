# 更新日志

所有版本的更新记录都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

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
