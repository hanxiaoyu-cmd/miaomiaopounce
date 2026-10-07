# 喵喵扑趣 · MiaoMiao Pounce

装在安卓平板上，给猫咪玩的离线二维捕猎小游戏。猫爪拍中移动的目标，会得到柔和的短音效和消失动画。

![主人界面](docs/images/home.png)

## 玩法与功能

- 四种原创矢量主题：小虫、小鱼、小老鼠、色块。
- 短距离移动、停顿、转向、偶尔加速；动物可以在遮挡物附近短暂躲藏。
- 1–8 个目标、三档速度、三档大小；1 / 3 / 5 分钟场次。
- 接触即命中、宽松判定、多点触摸、滑动捕获；目标不会在按住的爪子下持续刷新。
- 低音量反馈、静音和安静观察预设；声音限流，避免连续拍击时叠加过多。
- 长按左上角 1.6 秒并回答小问题，进入主人暂停菜单。
- 后台暂停、返回后确认继续、自动结束、当前设备上的设置和互动记录。
- 猫爪触摸诊断，显示触点、轨迹、落爪次数和中心目标命中次数。
- 系统屏幕固定引导；全屏游戏；支持横屏、竖屏、不同大小的平板。
- 无广告、无账户、无网络权限；不使用摄像头和麦克风。

## 安装

下载 [v1.0.0 安卓安装包](https://github.com/hanxiaoyu-cmd/miaomiaopounce/releases/download/v1.0.0/miaomiaopounce-1.0.0.apk)，传到平板打开安装。允许文件管理器安装该 APK 后即可使用。最低 Android 8.0（API 26）。[发布页](https://github.com/hanxiaoyu-cmd/miaomiaopounce/releases/tag/v1.0.0)同时提供 SHA-256 校验文件。

先打开「猫爪触摸测试」：用手指检查，再让猫自愿接触。看到圆圈和轨迹，说明平板确实上报了触摸。真实猫爪的识别效果受触控硬件、接触方式及保护膜影响，扩大软件判定范围不能修复硬件未上报的触摸。

建议先用「安静观察」预设，平板平放、防滑，并使用保护壳和屏幕保护。猫可以随时离开。屏幕缺少实际抓握触感，场次结束后可接一段实体玩具互动。

## 开发与构建

依赖：JDK 17 或 21、Android SDK Platform 36、Build Tools 35.0.0。工程固定使用 AGP 8.11.1、Kotlin 2.1.20、Gradle 8.13；Wrapper 包含官方分发 SHA-256 校验。

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease
```

Windows 使用 `gradlew.bat`。Android Studio 可以直接打开此目录；命令行构建设置 `ANDROID_HOME`，或在不提交的 `local.properties` 中配置 SDK 路径。Debug 的应用 ID 为 `com.miaomiaopounce.debug`，可与正式版同时安装，互不覆盖设置和签名。

网络访问 Maven Central 不稳定时，可显式设置 `MIAO_USE_MIRROR=1`，启用阿里云 Google / Maven 镜像；默认使用官方源。首次 Wrapper 下载若网络受限，可下载 Gradle 8.13 官方分发包并核对 Wrapper 中的 SHA-256 后运行其 `gradle` 命令。

Release 签名通过不提交的 `signing.properties` 配置：

```properties
storeFile=/absolute/path/to/private-release.jks
storePassword=YOUR_PRIVATE_PASSWORD
keyAlias=YOUR_ALIAS
keyPassword=YOUR_PRIVATE_PASSWORD
```

未提供配置时，`assembleRelease` 输出未签名 APK；Debug APK 可直接用于开发。仓库中没有签名私钥和密码。已发布的 APK 使用本次开发机器持有的独立 release 证书，后续更新需使用同一证书。

## 工程结构

```text
app/src/main/java/com/miaomiaopounce/
  game/           无安卓依赖的运动、命中、刷新和计时逻辑
  ui/             原创 Canvas 绘图、游戏画面和触摸测试
  audio/          SoundPool 预加载和音量、并发限制
  data/           本地设置和互动记录
  MainActivity.kt 主人界面、生命周期、暂停与结果
app/src/test/          运动与命中逻辑测试
app/src/androidTest/   安卓交互验收测试
scripts/generate_audio.py 原创反馈音效生成器
```

图形由代码绘制，声音由本仓库脚本生成，无下载的商业美术或音效素材。

## 验收与限制

具体结果见 [验收报告](docs/ACCEPTANCE.md)，真机步骤见 [小米平板验收清单](docs/TABLET_CHECKLIST.md)。GitHub Actions 自动运行构建、Lint、逻辑测试和 Android 12 / Android 16 平板模拟器交互测试。

软件验收不能证明所有猫都会喜欢。落爪计数也不能直接推断情绪：不出爪的猫可能正在观察。根据实际猫咪表现调整目标、节奏、数量和声音。

设计参考：[Battersea 猫咪玩耍建议](https://www.battersea.org.uk/pet-advice/cat-advice/playing-your-cat)、[Cats Protection 玩耍建议](https://www.cats.org.uk/help-and-advice/cat-behaviour/cats-and-play)。
