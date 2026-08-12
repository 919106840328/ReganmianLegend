# Android Studio 开发环境说明

## 已准备

| 项 | 路径 |
|----|------|
| JDK 17 | `C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot` |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk`（含 platform 35、build-tools） |
| **请用此工程打开** | `C:\Users\EDY\ReganmianLegend`（纯英文路径，可编译） |

桌面 `热干面传奇` 含中文，Windows 上 Kotlin 易编译失败；**Android Studio 请打开英文目录副本**。

## 首次打开步骤

1. 启动 Android Studio  
2. **Open** → 选 `C:\Users\EDY\ReganmianLegend`  
3. 若提示 Trust Project → Trust  
4. 等待 Gradle Sync（已配腾讯云 Gradle 镜像）  
5. 顶部设备选 Emulator 或真机 → 点 Run（绿色三角）

## SDK / JDK（若向导询问）

- Android SDK location：`C:\Users\EDY\AppData\Local\Android\Sdk`  
- JDK：选 Embedded JDK，或上述 Microsoft JDK 17  

## 与 Cursor 协作

- Cursor 可继续编辑桌面 `热干面传奇` 文档与代码  
- 改完代码后，把源码同步到 `C:\Users\EDY\ReganmianLegend` 再在 Studio 运行  
- 或直接把 Cursor 工作区也切到 `C:\Users\EDY\ReganmianLegend`（推荐，两边一致）

## 同步命令（改完桌面工程后）

```bat
robocopy "C:\Users\EDY\Desktop\热干面传奇" "C:\Users\EDY\ReganmianLegend" /E /XD .gradle build app\build .idea .kotlin /XF *.apk
```
