# test Location — Android 模拟定位 APP，可以修改手机位置在任何地方

> ## 📥 下载完整 APP
> ### 🔗 直连下载（.apk，仅 169KB）：[https://dw.locati.xyz/download/apk](https://dw.locati.xyz/download/apk)
> **官网下载页**：[https://dw.locati.xyz/download](https://dw.locati.xyz/download)
> 当前版本 **1.18** · Android 5.0+ · **无需越狱**

一款**轻量级**安卓模拟定位工具：**无需越狱、无需 Root、安装包仅 169KB**。打开地图选取一个点位，点击模拟，手机位置就到那里了——就这么简单。

### 核心亮点

- 🪶 **超轻量**：安装包仅 **169KB**，不占内存、不耗流量，老手机也能流畅运行
- 🔓 **无需越狱 / Root**：基于 Android 官方「模拟位置」机制，普通权限即可使用
- 🗺️ **地图选点**：内置离线地图，任意位置都能精确选取，弱网环境也能用
- 🖱️ **一键模拟**：选点 → 点击「开始模拟」→ 立即生效，1 秒持续下发定位
- ⭐ **收藏与最近位置**：常用地点收藏，一键切换；自动记录最近位置
- 🌐 **多语言**：内置中 / 英 / 日 / 韩 / 德 / 法 / 俄 界面语言

### 三步开始

1. 下载安装包，打开 App
2. 设置 → 开发者选项 → 「模拟位置信息应用」选择 test Location
3. 打开地图选一个点位，点击 **开始模拟** —— 完成 ✅

### 目录结构

```
.
├── app/                        # Android 工程（包名 com.test.mocklocation）
│   ├── src/main/java/com/test/mocklocation/
│   │   ├── MainActivity.java           # 主界面：开始 / 停止模拟
│   │   ├── MapActivity.java            # 地图选点
│   │   ├── MockLocationService.java    # 前台服务：持续下发模拟定位
│   │   ├── LocationHelper.java         # GPS / 网络定位 provider 封装
│   │   ├── FavoritesManager.java       # 收藏地点
│   │   ├── RecentLocationManager.java  # 最近位置
│   │   └── ...                         # 其余工具类
│   ├── src/main/assets/            # 离线地图（Leaflet）
│   └── src/main/res/               # 布局 / 多语言资源
├── build.gradle / settings.gradle  # Gradle 工程
├── build.sh                        # 一键构建（无需 Android Studio）
├── docs/                           # 详细文档
└── LICENSE                         # MIT
```

### 构建 APK（可选）

前置：Android SDK（`build-tools/33.0.2`、`platforms/android-33`）+ JDK 17。

```bash
git clone https://github.com/aiaiks7200/test-location-app.git
cd test-location-app
bash build.sh
# 产物：build/test-location.apk
```

详见 [`docs/BUILD.md`](docs/BUILD.md)。

### 文档

| 文档 | 说明 |
|---|---|
| [`docs/BUILD.md`](docs/BUILD.md) | 构建、签名、发布流程 |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 技术架构、核心类职责、定位下发机制 |
| [`docs/FAQ.md`](docs/FAQ.md) | 常见问题（定位不生效、兼容、重装等） |
| [`CHANGELOG.md`](CHANGELOG.md) | 版本记录 |

---

# test Location — Android Mock Location APP, set your phone location anywhere

> ## 📥 Download the full APP
> ### 🔗 Direct download (.apk, only 169KB): [https://dw.locati.xyz/download/apk](https://dw.locati.xyz/download/apk)
> **Official download page**: [https://dw.locati.xyz/download](https://dw.locati.xyz/download)
> Current version **1.18** · Android 5.0+ · **No root required**

A **lightweight** Android mock-location tool: **no root, no jailbreak, only 169KB in size**. Pick a spot on the map, tap simulate, and your phone is there — that's it.

### Highlights

- 🪶 **Tiny**: the whole APK is just **169KB** — minimal memory, minimal traffic, runs smoothly on old devices
- 🔓 **No root / no jailbreak**: uses Android's official "mock location" mechanism with normal permissions
- 🗺️ **Map-based selection**: built-in offline map for precise point selection, works even with a weak connection
- 🖱️ **One-tap simulation**: pick a point → tap **Start** → instant effect, with continuous 1-second location updates
- ⭐ **Favorites & recent locations**: save your usual spots and switch with one tap; recent locations are tracked automatically
- 🌐 **Multilingual**: built-in UI in EN / 中文 / 日本語 / 한국어 / Deutsch / Français / Русский

### Get started in 3 steps

1. Install the APK and open the app
2. Settings → Developer options → set **test Location** as the "Mock location app"
3. Pick a point on the map and tap **Start** — done ✅

### Build the APK (optional)

Prerequisites: Android SDK (`build-tools/33.0.2`, `platforms/android-33`) + JDK 17.

```bash
git clone https://github.com/aiaiks7200/test-location-app.git
cd test-location-app
bash build.sh
# Output: build/test-location.apk
```

See [`docs/BUILD.md`](docs/BUILD.md) for details.

---

## License

[MIT](LICENSE) — for learning and development/debugging use; please comply with local laws and regulations.
