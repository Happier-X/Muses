# 移除首页和音乐库功能

## Goal

简化 App 结构，移除首页和音乐库列表页面，使用户直接进入歌曲页面。

## Requirements

### 功能移除

1. **移除首页**
   - 删除 `lib/pages/home/home_page.dart`
   - 删除 `lib/pages/home/recent_playback_page.dart`
   - 删除整个 `lib/pages/home/` 目录

2. **移除音乐库列表页**
   - 删除 `lib/pages/library/albums_page.dart`（仅被首页和路由引用）
   - 删除 `lib/pages/library/artists_page.dart`（仅被首页和路由引用）
   - 保留 `lib/pages/library/playlists_page.dart`（被歌曲详情、播放器等多处引用）
   - 保留 `lib/pages/library/library_detail_pages.dart`（详情页被多处引用）

### 路由更新

1. **修改初始路由**
   - `AppRouter.initialRoute` 改为 `AppRoutes.songs`
   - 移除 `AppRoutes.home` 路由定义
   - 移除 `AppRoutes.albums` 路由定义
   - 移除 `AppRoutes.artists` 路由定义

2. **清理路由常量**
   - 移除 `AppRoutes.home`
   - 移除 `AppRoutes.albums`
   - 移除 `AppRoutes.artists`

### 侧边菜单更新

1. **移除菜单项**
   - 移除"音乐库"菜单项（指向 `/home`）
   - 移除"专辑"菜单项
   - 移除"艺术家"菜单项
   - 保留"歌曲"、"歌单"、"音源"、"统计"、"设置"

### 代码清理

1. **清理导入**
   - 移除所有对已删除页面的导入引用
   - 确保无编译错误

## Acceptance Criteria

- [ ] App 启动后直接进入歌曲页面
- [ ] 侧边菜单不再显示"音乐库"、"专辑"、"艺术家"
- [ ] 歌曲详情、播放器面板中的歌单选择功能正常工作
- [ ] 歌单详情页、艺术家详情页、专辑详情页可从其他入口正常访问
- [ ] `flutter analyze` 无错误
- [ ] `flutter test` 通过

## Scope

### 保留的页面

- `lib/pages/library/playlists_page.dart` - 歌单列表（被多处引用）
- `lib/pages/library/library_detail_pages.dart` - 详情页（被多处引用）

### 保留的路由

- `/songs` - 歌曲页面（新初始路由）
- `/playlists` - 歌单列表
- `/player` - 播放器
- `/source` - 音源
- `/settings` - 设置及相关子页面
- `/search` - 搜索

## Notes

- 首页包含的自动播放逻辑（`_tryAutoPlayOnAppLaunch`）和库刷新逻辑（`_tryRefreshLibraryOnLaunch`）将迁移到 `SongsPage`
- 迁移时需保持功能不变，包括 `mounted` 检查和错误处理
