# 常见问题 / FAQ

## 定位 / 模拟 / Location

**Q：开始模拟后，其他 App 定位不变？**
- 确认在「设置 → 开发者选项 → 模拟位置信息应用」里选择了 **test Location**。
- 本 App 同时 mock 了 GPS 和网络定位，但个别 App 会缓存上一次位置，杀掉该 App 重开再试。
- 部分 App（尤其地图类）有自己的定位 SDK 与防模拟逻辑，效果因 App 而异。

**Q：为什么定位会突然回到真实位置？**
- 前台服务被系统杀死后下发会中断。请把本 App 加入电池优化白名单（不优化/无限制）。
- 部分国产 ROM 需要允许「自启动」并加入后台白名单。

**Q：弱网或没有网络时能用吗？**
- 能。地图与瓦片打包在应用内（离线 Leaflet），选点不依赖网络；已开始的模拟定位也在本地持续下发。

**A (EN): Mocked location doesn't apply to other apps?**
- Confirm **test Location** is selected under Settings → Developer options → "Mock location app".
- Some apps cache the last fix — force-stop and reopen them.
- Apps with proprietary location SDKs may behave differently.

**A (EN): Works offline?**
- Yes. The map and tiles are bundled in the APK; picking a point needs no network.

## 安装 / 更新 / Install

**Q：需要 Root 或越狱吗？**
- 不需要。基于 Android 5.0+ 官方「模拟位置」机制，普通权限即可。

**Q：更新版本提示签名不一致？**
- 说明新 APK 用了不同 keystore 签名。请使用同一 `release.keystore` 构建发版（见 `docs/BUILD.md`）。

**Q：安装包为什么只有 169KB？**
- 命令行极简构建（无重型依赖）+ 内置轻量离线地图，全部资源压缩打包，所以体积很小、内存占用低。

**A (EN): Root / jailbreak required?**
- No. It uses Android's official mock-location mechanism (Android 5.0+).

**A (EN): Why is the APK only 169KB?**
- A minimal command-line build with no heavy dependencies and a bundled lightweight offline map — small footprint, low memory.

## 联系 / Contact

- 作者邮箱：`aiaiks720@gmail.com`
- 反馈请附：App 版本号、设备型号、Android 版本。
- Feedback: please include app version, device model, and Android version.
