# 更新日志 / Changelog

> 本仓库基线版本为 **1.13**（`versionCode 14`）。线上最新版本以服务端「版本管理」为准（当前 1.18）。

## 1.13（本仓库基线 / This repository baseline）

- 安全加固：APK 签名指纹校验（`SignatureVerifier`）、防重装绕过（`AppProtector`）
- 敏感字符串运行时混淆（`StringObfuscator`），密钥服务端化，APK 内不含密钥
- 离线地图（Leaflet）选点，弱网环境可用
- 多语言界面（中 / 英 / 日 / 韩 / 德 / 法 / 俄）

## 1.12

- 离线地图选点（`assets/map.html`）
- 收藏地点 / 最近位置管理
- 多语言资源

## 1.08

- 最近位置记录数量与去重阈值优化

## 早期版本（历史概要）

- **1.05**：服务端同步逻辑完善
- **1.04**：授权同步与版本逻辑增强
- **1.03**：弹窗交互修复；强制升级
- **1.02**：页面跳转修复
- **1.01**：启动拦截、设置页信息、订单去重、设备码绑定

---

Security hardening (signature verification, anti-tamper, string obfuscation) has been in place since 1.12+. Full version history is managed by the server's version system.
