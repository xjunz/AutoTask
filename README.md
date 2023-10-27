
# AutoTask

一款支持[Shizuku](https://github.com/RikkaApps/Shizuku)和辅助功能的**自动任务**工具，[点击下载体验最新版](https://www.coolapk.com/apk/935131)。

## 简介

本应用专注于帮助您执行自动任务，相比于其他同类产品，本应用具有以下特点：

- 支持多种启动模式（**Shizuku**和辅助功能）
- 支持自定义常驻任务任务和一次性任务
- 支持手势录制，审查布局树等
- 不需要刻意保活便可常驻后台（两种模式默认系统保活）
- 省电且占用系统资源较少（事件驱动+协程，执行长时间任务也不阻塞CPU）
- 代码开源，安全可信
- Material 3 风格UI，实用美观

## 截图

| <img src="/app/screenshots/Screenshot_light_1.png" alt="pic_main" style="zoom:25%;" /> | <img src="/app/screenshots/Screenshot_light_2.png" alt="pic_test" style="zoom:25%;" /> | <img src="/app/screenshots/Screenshot_night_1.png" style="zoom:25%;" /> | <img src="/app/screenshots/Screenshot_night_2.png" style="zoom:25%;" /> |
|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|

## 实现

### Shizuku模式

利用Shizuku授予特权，使用安卓内置的 [UiAutomation](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/UiAutomation.java)框架用于任务执行，详见 [ShizukuAutomatorService](https://github.com/xjunz/AutoTask/blob/master/app/src/main/java/top/xjunz/tasker/service/ShizukuAutomatorService.kt)。

> **注: **因为安卓系统只能注册一个`UiAutomation`服务，所以当自动任务服务激活时，其他`UiAutomation`会注册失败。如果您有需要（如自动化测试、Thanox），请先停止自动任务服务。反之亦然。
### 辅助功能模式

使用辅助功能自带的API框架用于实现任务执行，详见[A11yAutomatorService](https://github.com/xjunz/AutoTask/blob/master/app/src/main/java/top/xjunz/tasker/service/A11yAutomatorService.kt)。

## 构建

如果你想构建此项目，请将代码`git clone`到本地后，在**Android Studio**打开，并在项目根目录下建立`local.properties`(如果不存在的话)，在其中配置自定义签名信息:

```properties
storeFile=xxx
storePassword=xxx
keyAlias=xxx
keyPassword=xxx
```

## 注意事项

本项目仅供学习交流使用，禁止用于商业用途或非法用途！

## License

本应用基于[Apache-2.0 License](https://github.com/xjunz/AutoSkip/blob/master/LICENSE)开源，请在开源协议约束范围内使用源代码 。

*Copyright 2023 XJUNZ*
