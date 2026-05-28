# 彻底移除我喜欢功能

## Goal

彻底移除应用中的"我喜欢"功能，包括所有相关代码和数据库字段。

## Requirements

- 移除 `PlaylistEntity` 中的 `isFavorite` 字段
- 移除 `PlaylistsService` 中的所有 `favorite` 相关代码
- 移除 `settings_notification_state.dart` 中的 `showFavoriteAction` 设置
- 移除 `db_helper.dart` 中的 `isFavorite` 字段
- 更新数据库迁移逻辑
- 确保应用可以正常编译和运行

## Acceptance Criteria

- [ ] 所有 `favorite` 相关代码已移除
- [ ] 数据库中的 `isFavorite` 字段已移除
- [ ] 应用可以正常编译
- [ ] 应用可以正常运行
- [ ] 普通歌单功能不受影响

## Notes

- 需要谨慎处理数据库迁移，避免数据丢失
- 移除功能后需要测试其他功能是否正常
