# 林默注入器 - 手机APK打包指南

## 在手机上打包APK

### 方法一：使用 AIDE（推荐）
1. 下载安装 **AIDE - Android IDE** (Google Play/各大应用市场)
2. 在 AIDE 中打开项目：
   - `菜单 → Git → Clone →` 输入仓库地址
3. 点击 ▶ **运行**按钮
4. 选择 **Compile & Install**
5. 等待编译完成，APK 会自动安装

### 方法二：使用 APK Tool M + AAPT2 (进阶)
如果你有 `APK Tool M` 或 `APK Editor Pro`，也可以直接打包。

---

## 功能说明

- **灵动岛悬浮窗**：丝滑的展开/收起动画
- **音量键控制**：按音量+打开悬浮窗，音量-关闭悬浮窗
- **导入功能**：自动复制 `/storage/emulated/0/林默自动注入/` 下的所有 .pak 文件
- **导入原版**：恢复原版 PAK 备份（在 `/storage/emulated/0/林默自动注入/原版/`）

---

## 项目结构

```
LinMoInjector/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/linmo/injector/
│       │   ├── MainActivity.java
│       │   ├── FloatingWindowService.java
│       │   └── VolumeKeyReceiver.java
│       └── res/
│           ├── layout/
│           ├── drawable/
│           └── values/
├── README.md
├── build.gradle
└── settings.gradle
```
