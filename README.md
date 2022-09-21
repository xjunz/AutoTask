
# AutoSkip

一款基于[Shizuku](https://github.com/RikkaApps/Shizuku)授权**自动跳过**工具

## 简介

本应用专注于帮助您跳过应用的启动广告/宣传页，相比于其他同类产品，本应用具有以下特点：

## 特点

- 不需要您手动开启辅助功能
- 支持开机自启
- 不需要在通知栏显示通知以保持后台运行
- 不需要刻意保活便可常驻后台
- 省电且占用系统资源较少
- 代码开源，安全可信

## 截图

| <img src="/app/screenshots/pic_main.webp" alt="pic_main" style="zoom:25%;" /> | <img src="/app/screenshots/pic_test.webp" alt="pic_test" style="zoom:25%;" /> | <img src="/app/screenshots/pic_records.webp" style="zoom:25%;" /> | <img src="/app/screenshots/pic_night_mode.webp" style="zoom:25%;" /> |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |

## 实现

利用Shizuku授予特权，使用安卓内置的 [UiAutomation](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/UiAutomation.java)
框架用于目标识别和模拟点击。详见 [AutomatorConnection.kt](https://github.com/xjunz/AutoSkip/blob/master/automator/src/main/java/top/xjunz/automator/AutomatorConnection.kt)。
**注: **因为安卓系统只能注册一个UiAutomation服务，所以当自动跳过服务激活时，其他UiAutomation会注册失败。如果您有需要（如自动化测试），请先停止自动跳过服务。
## 构建

如果你想构建此项目，请将代码`git clone`到本地后，在**Android Studio**打开，并

- 手动建立`top.xjunz.automator.Constants.kt`类，并在其中定义:

```kotlin
const val ALIPAY_DONATE_URL ="xxx"
const val EMAIL_ADDRESS = "xxx"
const val APP_DOWNLOAD_URL = "xxx"
const val FEEDBACK_GROUP_URL = "xxx"
```

- 在项目根目录下建立`local.properties`(如果不存在的话)，在其中配置自定义签名信息:

```properties
storeFile=xxx
storePassword=xxx
keyAlias=xxx
keyPassword=xxx
```

## License

> 本应用基于[Apache-2.0 License](https://github.com/xjunz/AutoSkip/blob/master/LICENSE)开源，请在开源协议约束范围内使用源代码 | **Copyright 2021 XJUNZ**
>

