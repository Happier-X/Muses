# WebDAV添加时支持选择文件夹

## Goal

改进WebDAV音源添加流程，让用户在添加时就能选择扫描文件夹，而不是添加后进入编辑模式才能选择。使添加流程更直观、高效。

## Confirmed Facts

- 当前添加WebDAV时，`WebDavEditPage` 在 `isAdd: true` 模式下隐藏文件夹选择区域（`webdav_edit_page.dart:266`）
- 文件夹选择器 `WebDavFolderPickerPage` 已存在，支持多选远程目录
- WebDAV编辑页面已有完整的文件夹选择UI（扫描文件夹、排除文件夹）
- 添加流程：连接测试 → 保存 → 返回列表；编辑流程：可选择文件夹 → 保存

## Requirements

- 添加WebDAV时显示文件夹选择区域（与编辑模式一致）
- 保留连接测试功能（测试成功后才能保存）
- 保留默认路径字段（用于初始连接和目录浏览）
- 文件夹选择为可选（用户可跳过，使用默认路径）

## Acceptance Criteria

- [ ] 添加WebDAV时，连接测试成功后显示文件夹选择区域
- [ ] 文件夹选择器可正常打开、浏览、多选
- [ ] 选择的文件夹正确保存到 `includeFolders` 字段
- [ ] 添加完成后，音源列表正确显示选中的文件夹
- [ ] 不影响现有编辑模式的文件夹选择功能

## Design Decisions

- 文件夹选择区域始终显示（与编辑模式一致），不依赖连接测试结果
- 如果用户未选择文件夹，使用 `path` 字段作为默认扫描路径（代码已支持：`webdav_music_service.dart:123-125`）

## Open Questions

- 无

## Notes

- 技术实现相对简单：移除 `isAdd` 条件判断，调整UI布局
- 需要确保连接测试失败时文件夹选择区域不显示或禁用
