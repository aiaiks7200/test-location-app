# 构建指南

## 环境要求

| 组件 | 版本 |
|---|---|
| Android SDK | `build-tools/33.0.2`、`platforms/android-33` |
| JDK | 17（`/usr/lib/jvm/java-17-openjdk-amd64`） |
| 工具 | `javac`、`zip`、`keytool`、`bash` |

## 一键构建

```bash
export ANDROID_HOME=~/android-sdk        # 按你的 SDK 路径
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
bash build.sh
```

产物：`build/test-location.apk`。

### build.sh 做了什么

`build.sh` 不依赖 Android Studio / Gradle 完整工具链，走的是**命令行最小构建**：

1. `aapt2 compile` 编译 res 资源
2. `aapt2 link` 链接资源 + 生成 R.java
3. 把 `assets/`（Leaflet 离线地图）打进 APK
4. `javac` 编译所有 Java（source/target 8）
5. `d8` 转 DEX（min-api 21）
6. 组装 APK
7. `apksigner` 签名

## 签名

- **开发**：项目根没有 `release.keystore` 时，脚本会生成一个临时 keystore（密码默认 `testmock123`，可用 `KEYSTORE_PASS` 覆盖）。注意：临时 keystore 与正式 keystore 签名不同，已安装用户更新时会提示签名冲突。
- **发版**：把正式 `release.keystore` 放到项目根目录，并：
  ```bash
  export KEYSTORE_PASS='你的keystore密码'
  bash build.sh
  ```
  签名 alias 固定为 `testmock`。

> ⚠️ 妥善保管你的 keystore，丢失将无法发布兼容旧版的更新。`.gitignore` 已默认排除 `*.keystore`。

## 用 Android Studio 打开

工程是标准 Gradle 工程，也可以直接用 Android Studio 打开根目录，选择 `app` 模块，Run 或 `Build APK` 即可。`build.sh` 只是提供了一条无需 IDE 的轻量路径。

## 验证 APK

```bash
$ANDROID_HOME/build-tools/33.0.2/apksigner verify --verbose build/test-location.apk
$ANDROID_HOME/build-tools/33.0.2/aapt2 dump badging build/test-location.apk | head
```
