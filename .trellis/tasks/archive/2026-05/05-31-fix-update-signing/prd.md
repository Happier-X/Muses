# 修复应用内更新签名不匹配问题

## Goal

修复从应用内更新时，安装提示签名不匹配的问题。

## 问题描述

- **现象**：从应用内更新时，安装提示签名不同
- **影响**：无法完成应用内更新，用户体验差

## 已知信息

1. **构建流程**：GitHub Action 构建，使用 `--flavor prod` 构建生产版本
2. **签名配置**：在 `android/app/build.gradle.kts` 中配置
3. **签名密钥**：通过 GitHub Secrets 管理（`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_PASSWORD`、`ANDROID_KEY_ALIAS`）

## 根本原因

**属性名不匹配**：

- `build.gradle.kts` 期望的属性名：`SIGNING_STORE_FILE`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`
- GitHub Action 创建的 `key.properties` 文件中的属性名：`storePassword`、`keyPassword`、`keyAlias`、`storeFile`

由于属性名不匹配，签名配置无法正确读取，导致使用了 debug 签名或签名配置错误。

## 修复方案

修改 GitHub Action 中的 `key.properties` 文件，使用正确的属性名：

**修复前**：
```
storePassword=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
keyPassword=${{ secrets.ANDROID_KEY_PASSWORD }}
keyAlias=${{ secrets.ANDROID_KEY_ALIAS }}
storeFile=${{ github.workspace }}/release.keystore
```

**修复后**：
```
SIGNING_STORE_PASSWORD=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
SIGNING_KEY_PASSWORD=${{ secrets.ANDROID_KEY_PASSWORD }}
SIGNING_KEY_ALIAS=${{ secrets.ANDROID_KEY_ALIAS }}
SIGNING_STORE_FILE=${{ github.workspace }}/release.keystore
```

## 验证方法

1. 推送新版本标签，触发构建
2. 下载构建产物，检查签名信息
3. 安装到设备上，测试应用内更新

## 需要探索的问题

1. **签名配置检查**：检查 `android/app/build.gradle.kts` 中的签名配置
2. **密钥验证**：确认 GitHub Secrets 中的密钥是否正确
3. **构建日志**：检查 GitHub Action 构建日志，确认签名是否正确应用
4. **已安装应用**：确认设备上已安装应用的签名信息

## 范围

- 修复签名不匹配问题，确保应用内更新正常工作
- 不修改应用功能代码
- 不更换签名密钥（除非必要）

## 验收标准

- [ ] 应用内更新不再提示签名不匹配
- [ ] 构建产物签名正确
- [ ] 修复方案可维护
