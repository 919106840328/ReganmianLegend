# 热干面传奇（ReganmianLegend）

一款以武汉热干面早餐档为背景的 **Android 时间管理 + 轻模拟经营** 游戏。玩家在当日倒计时内按订单完成「掂面 → 调酱 → 烫面 → 加料 → 出餐 → 收钱」，再用金币升级设备。

品类对标《沙威玛传奇》（Shawarma Legend），视觉基调是手绘卡通与武汉早市烟火气。

## 技术栈

- 语言 / UI：Kotlin + Jetpack Compose
- 架构：Compose UI → 纯 Kotlin 游戏逻辑层（`GameState` / `GameIntent` / `GameReducer` / `TickEngine`） → DataStore / JSON 关卡
- 关卡：`app/src/main/assets/levels/day_01.json` – `day_10.json`
- 最低安卓 SDK 以工程 Gradle 为准

## 目录说明

| 路径 | 说明 |
|------|------|
| `app/` | Android 应用源码 |
| `app/src/main/java/com/regannian/legend/game/` | 游戏状态、流水线、升级与 Tick |
| `app/src/main/java/com/regannian/legend/ui/` | Compose 画面 |
| `热干面传奇_GDD完整版.md` | 游戏设计文档入口 |
| `热干面传奇_GDD设计主体.md` | 玩法 / 升级 / 关卡细则 |
| `热干面传奇_初步技术方案.md` | 架构与实现细则 |
| `A1构建说明.md` | 命令行编译与验收 |
| `AndroidStudio环境说明.md` | Android Studio 打开步骤 |

## 本地运行

1. 用 Android Studio 打开本仓库根目录。
2. 等待 Gradle Sync 完成（JDK 17 + Android SDK）。
3. 选择模拟器或真机，点 Run。

命令行（Windows）：

```bat
gradlew.bat assembleDebug
```

Debug APK 输出：`app/build/outputs/apk/debug/`

建议工程路径不要含中文，否则 Kotlin/资源编译在 Windows 上容易出问题。

## 状态

当前以灰盒 / 玩法 MVP 为主：10 日关卡配置已在仓库中。完整升级树、辅食与结局亍在本仓库的首发范围里。
