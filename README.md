# test Location — Android 模拟定位 APP

带**在线会员授权 + App 内付费续费**的安卓模拟定位应用。每台设备 15 天免费试用，到期后可在 App 内直接购买月度 / 年度会员继续使用；会员有效期以服务端为准，重装 App 不重置。

- 包名：`com.test.mocklocation`
- 最低 Android：API 21（Android 5.0）
- 授权 / 服务端域名在 `app/src/main/java/com/test/mocklocation/StringObfuscator.java` 中混淆维护

## 功能

- 🗺️ **模拟定位**：同时 mock `GPS_PROVIDER` 与 `NETWORK_PROVIDER`，前台服务持续下发定位
- 📍 **离线地图选点**：内置 Leaflet + 离线瓦片方案（`assets/map.html`），弱网环境可用
- ⭐ **收藏 / 最近位置**：常用地点一键切换，自动记录最近位置
- 🔐 **设备码会员**：设备唯一码绑定，服务端判定到期，本地仅作离线兜底
- 💳 **App 内付费**：支付宝 / 微信（H5）下单，实时读取服务端动态价格，无需升级 APP
- 📈 **强制升级**：服务端可推送新版本并强制升级
- 🛡️ **安全加固**：签名校验、防重装绕过、混淆、密钥全部服务端化

## 目录结构

```
.
├── app/                        # Android 工程
│   ├── src/main/java/com/test/mocklocation/
│   │   ├── MainActivity.java           # 主界面：试用/会员状态、开始模拟
│   │   ├── MapActivity.java            # 地图选点
│   │   ├── PaymentActivity.java        # 支付页：套餐选择、支付宝/微信
│   │   ├── SettingsActivity.java       # 设置：会员信息、关于
│   │   ├── MockLocationService.java    # 前台服务：持续下发模拟定位
│   │   ├── LocationHelper.java         # 定位 provider 封装
│   │   ├── LicenseApiHelper.java       # 服务端授权：check / 订单注册
│   │   ├── PlanApiHelper.java          # 动态价格：/api/plans
│   │   ├── PaymentApiHelper.java       # 支付：创建订单 / 轮询结果
│   │   ├── TrialManager.java           # 15 天试用管理
│   │   ├── FavoritesManager.java       # 收藏地点
│   │   ├── RecentLocationManager.java  # 最近位置
│   │   ├── SignatureVerifier.java      # 签名校验
│   │   ├── AppProtector.java           # 防重装/加固
│   │   ├── StringObfuscator.java       # 敏感字符串混淆
│   │   └── MD5Util.java
│   ├── src/main/assets/            # Leaflet 离线地图
│   └── src/main/res/               # 布局 / 多语言(zh,en,ja,ko,de,fr,ru)
├── build.gradle / settings.gradle  # Gradle 工程
├── build.sh                        # 一键构建（无需 Android Studio）
├── docs/                           # 详细文档
└── LICENSE                         # MIT
```

## 快速开始（构建 APK）

前置：Android SDK（`build-tools/33.0.2`、`platforms/android-33`）+ JDK 17。

```bash
# 1. 克隆
git clone https://github.com/aiaiks720/test-location-app.git
cd test-location-app

# 2. 一键构建
bash build.sh
# 产物：build/test-location.apk
```

> 正式发版请把你的 `release.keystore` 放进项目根目录，并设置 `KEYSTORE_PASS` 环境变量；
> 否则会生成临时 keystore（仅开发用，已安装用户会视为不同应用）。

详见 [`docs/BUILD.md`](docs/BUILD.md)。

## 使用说明

1. 安装 APK 后打开 App，开发者选项中选择本 App 为「模拟位置信息应用」。
2. 首次启动获得 15 天免费试用，主页显示剩余天数。
3. 试用到期后进入支付页，选择月度或年度会员，用支付宝 / 微信完成支付。
4. 支付成功后会员自动续期：月付 +30 天、年付 +365 天（在当前到期日基础上叠加）。
5. 开始模拟定位后，App 以 1 秒间隔持续向 `GPS` 与 `NETWORK` provider 下发所选坐标。

## 文档

| 文档 | 说明 |
|---|---|
| [`docs/BUILD.md`](docs/BUILD.md) | 构建、签名、发布流程 |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 技术架构、核心类职责、授权/支付流程 |
| [`docs/FAQ.md`](docs/FAQ.md) | 常见问题（定位不生效、支付、重装等） |
| [`CHANGELOG.md`](CHANGELOG.md) | 版本记录 |

## License

[MIT](LICENSE) — 仅供学习与开发调试用途，请遵守当地法律法规。
