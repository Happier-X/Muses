# 修复 dialog 与底部播放条的层级问题

## Goal

修复 dialog/bottom sheet 弹出时被底部迷你播放条遮挡的 UI 层级问题。

## Background

`MiniPlayerBar` 在 `TabletLayoutHost` 的 `Stack` 中渲染，位于 base Navigator **之后**。由于 Stack 按子组件顺序绘制，MiniPlayerBar 始终绘制在 Navigator overlay（dialog 所在位置）之上，导致所有 dialog/bottom sheet 的遮罩层和内容都被播放条遮挡。

## Requirements

1. 所有 `showDialog` / `showModalBottomSheet` 弹出的内容应显示在 MiniPlayerBar **之上**
2. 修复范围覆盖 tablet 模式和 phone 模式
3. 修复后可移除已有的手动 padding 补偿（如 `tabletOverlayInset`）

## Affected Files (51 call sites)

- `lib/pages/settings/app_appearance_settings_page.dart`
- `lib/pages/settings/version_info_page.dart`
- `lib/pages/settings/listening_stats_page.dart`
- `lib/pages/source/local_source_settings_page.dart`
- `lib/pages/source/source_page.dart`
- `lib/pages/source/local/local_folder_browser.dart`
- `lib/pages/source/webdav/webdav_folder_browser.dart`
- `lib/pages/source/webdav/webdav_edit_page.dart`
- `lib/pages/source/folder_songs_page.dart`
- `lib/pages/library/artists_page.dart`
- `lib/pages/library/albums_page.dart`
- `lib/pages/library/playlists_page.dart`
- `lib/pages/library/library_detail_pages.dart`
- `lib/pages/songs/songs_page.dart`
- `lib/pages/songs/song_detail_sheet.dart`
- `lib/pages/search/search_page.dart`
- `lib/pages/player/widgets/player_bottom_panel.dart`
- `lib/pages/player/lyrics/lyric_view.dart`
- `lib/components/dialog/app_dialog.dart`

## Acceptance Criteria

- [ ] dialog / bottom sheet 弹出时显示在 MiniPlayerBar 之上
- [ ] 遮罩层（modal barrier）覆盖整个屏幕包括 MiniPlayerBar
- [ ] tablet 模式和 phone 模式均正常
- [ ] 移除已有的手动 padding 补偿代码（`tabletOverlayInset` 等）
- [ ] 所有现有 dialog 功能不受影响（关闭、返回等）

## Notes

- 根因是 Stack 子组件顺序导致的 z-index 问题
- 需评估 `useRootNavigator: true` 方案的可行性（dialog pop 语义是否受影响）
