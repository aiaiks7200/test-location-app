# 技术架构（APP 视角）/ Technical Architecture

> 本文档描述 **APP 端**架构与定位下发机制。

## 模块划分

```
┌─────────────────────────────────────────────────────────────┐
│                        Activity 层                           │
│  MainActivity · MapActivity · SettingsActivity               │
└───────────────┬───────────────────────────────┬──────────────┘
                │ 状态/界面                      │ 选点
┌───────────────▼──────────────┐   ┌────────────▼──────────────┐
│     MockLocationService       │   │  MapActivity + Leaflet     │
│  前台服务 · 持续下发定位       │   │  离线地图 assets/map.html   │
│  GPS_PROVIDER + NETWORK       │   │  选点 → 坐标 → Service     │
└───────────────┬──────────────┘   └────────────────────────────┘
                │
┌───────────────▼──────────────────────────────────────────────┐
│                        业务层 / 工具层                         │
│  LocationHelper · FavoritesManager · RecentLocationManager    │
│  SignatureVerifier · AppProtector · StringObfuscator          │
└──────────────────────────────────────────────────────────────┘
```

## 核心类职责

| 类 | 职责 |
|---|---|
| `MainActivity` | 入口；开始 / 停止模拟；展示定位状态 |
| `MapActivity` | 离线地图选点（WebView 加载 `assets/map.html`），把坐标回传 Service |
| `SettingsActivity` | 设置：语言、关于 |
| `MockLocationService` | `ForegroundService`，按间隔向 GPS/NETWORK provider `setProvider`+`addLocation` |
| `LocationHelper` | `LocationManager` 封装，统一 mock 两个 provider |
| `FavoritesManager` / `RecentLocationManager` | 收藏地点 / 最近位置持久化（SharedPreferences） |
| `SignatureVerifier` | APK 签名指纹校验（防篡改/重打包） |
| `AppProtector` | 设备绑定与防重装 |
| `StringObfuscator` | 敏感字符串运行时拼接，降低静态反编译暴露 |

## 定位下发机制 / How mock location works

1. **双 provider**：必须**同时** mock `GPS_PROVIDER` 和 `NETWORK_PROVIDER`——只 mock 一个时，优先读网络定位的 App 仍会显示旧位置。
2. **持续下发**：`MockLocationService` 作为前台服务按 1 秒间隔循环 `addLocation`；只发一次会被系统判定失效并回落到真实定位。
3. **官方模拟位置机制**：Android 5.0+ 的「模拟位置信息应用」（`ACCESS_MOCK_LOCATION`）属于系统级官方能力，**不需要 Root / 越狱**。
4. **离线地图**：地图与瓦片打包在 `assets/`，通过 `file:///android_asset/map.html` 加载，不依赖在线地图服务，弱网/无网也可选点。

## 安全设计（APP 侧）

- `StringObfuscator` 运行时拼接敏感串，降低静态反编译暴露
- `SignatureVerifier` 校验签名指纹，被重打包/篡改的 APK 拒绝运行
- `AppProtector` 设备绑定，防止简单重装绕过
- 最小权限集：仅申请定位、前台服务、通知等必要权限

## Build notes (EN)

- Mocks both `GPS_PROVIDER` and `NETWORK_PROVIDER` continuously (1s interval) via a foreground service.
- Uses Android's official mock-location mechanism (`ACCESS_MOCK_LOCATION`) — **no root required**.
- Map & tiles ship inside `assets/` (Leaflet, loaded via `file:///android_asset/map.html`), so selection works offline.
- The tiny 169KB footprint comes from a command-line build (`build.sh`), no heavy libraries, and bundled offline map assets.
