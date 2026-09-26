# 技术架构（APP 前端视角）

> 本文档只描述 **APP 端**架构与它同授权服务端的**对接契约**。服务端实现细节不在此仓库内。

## 模块划分

```
┌─────────────────────────────────────────────────────────────┐
│                        Activity 层                            │
│  MainActivity · MapActivity · PaymentActivity · Settings      │
└───────────────┬───────────────────────────────┬──────────────┘
                │ 状态/界面                      │ 选点
┌───────────────▼──────────────┐   ┌────────────▼──────────────┐
│      MockLocationService      │   │  MapActivity + Leaflet     │
│  前台服务 · 持续下发定位       │   │  离线地图 assets/map.html   │
│  GPS_PROVIDER + NETWORK       │   │  选点 → 坐标 → Service     │
└───────────────┬──────────────┘   └────────────────────────────┘
                │
┌───────────────▼──────────────────────────────────────────────┐
│                      授权 / 业务层                             │
│  LicenseApiHelper  TrialManager  PlanApiHelper                │
│  PaymentApiHelper  FavoritesManager  RecentLocationManager    │
└───────────────┬──────────────────────────────────────────────┘
                │ HTTPS (JSON)
┌───────────────▼──────────────────────────────────────────────┐
│              授权 / 支付服务端（域名见 StringObfuscator）        │
└──────────────────────────────────────────────────────────────┘
```

## 核心类职责

| 类 | 职责 |
|---|---|
| `MainActivity` | 入口；展示试用/会员状态；启动模拟；到期拦截进支付页 |
| `MapActivity` | 离线地图选点（WebView 加载 `assets/map.html`），把坐标回传 Service |
| `PaymentActivity` | 支付页：读取动态价格、选择支付宝/微信、创建订单、轮询支付结果 |
| `SettingsActivity` | 会员信息、设备码、关于 |
| `MockLocationService` | `ForegroundService`，按间隔向 GPS/NETWORK provider `setProvider`+`addLocation` |
| `LocationHelper` | `LocationManager` 封装，统一 mock 两个 provider |
| `LicenseApiHelper` | `POST /api/license/check` 远程鉴权 + 版本/强制升级；本地短期缓存兜底 |
| `PlanApiHelper` | `GET /api/plans` 拉取月付/年付动态价格与是否启用微信 |
| `PaymentApiHelper` | `POST /api/pay/create` 下单、`POST /api/pay/query` 轮询、解析 H5/微信链接 |
| `TrialManager` | 15 天试用计时（`TRIAL_DAYS=15`） |
| `FavoritesManager` / `RecentLocationManager` | 收藏地点 / 最近位置持久化（SharedPreferences） |
| `SignatureVerifier` | APK 签名指纹校验（防篡改） |
| `AppProtector` | 防重装绕过：设备码与服务端绑定 |
| `StringObfuscator` | 服务端域名等敏感字符串运行时拼接，避免 APK 明文 |

## 会员 / 授权流程

1. **启动 / 打开地图 / 开始模拟前**都调用 `LicenseApiHelper.checkLicense()`。
2. 请求携带：设备码、`version_name`、`version_code`（从 `PackageManager` 动态读取，勿硬编码）。
3. 服务端返回：是否有效、到期时间、最新版本、是否强制升级。
4. 有效 → 进入；到期 → 弹支付页；`must_upgrade` → 引导下载新 APK。
5. 本地缓存仅作**短期离线兜底**，联网时以服务端为准。

## 付费 / 续期流程

1. `PaymentActivity` 打开即 `PlanApiHelper.fetchPlans()` 拉动态价格，渲染「月度 ¥x / 年度 ¥y」。
2. 用户选支付方式 → `PaymentApiHelper.createOrder(...)` 调 `/api/pay/create` 拿支付链接。
3. 支付宝走 `alipays://` 唤起；微信走 H5 页（服务端解析出内部 `weixin://` 链接）。
4. 支付完成后轮询 `/api/pay/query`，服务端确认后返回新的 `expire_at`。
5. APP 用服务端返回的 `expire_at` 同步本地订阅结束时间。
6. **续期规则（服务端执行）**：未过期会员月付 +30 天、年付 +365 天；已过期则从当前时间起算。

## 定位下发要点

- 必须**同时** mock `GPS_PROVIDER` 和 `NETWORK_PROVIDER`，否则部分 App 仍读网络定位。
- 必须**持续**发送（循环），只发一次会很快被系统判定失效。
- 用户需先在开发者选项把本 App 设为「模拟位置信息应用」。
- 地图资源走 `file:///android_asset/map.html`，不依赖国内不可达的地图 CDN。

## 安全设计（APP 侧）

- 支付网关地址、商户参数**不进 APK**，由服务端持有；APK 只与服务端授权域名通信。
- `StringObfuscator` 运行时拼接敏感串，降低静态反编译暴露。
- `SignatureVerifier` 校验签名指纹，被重打包/篡改的 APK 拒绝运行。
- `AppProtector` + 设备码服务端绑定，重装 App 不重置会员/试用。
