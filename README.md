# FocusIntent

专注意图 - 通过明确使用目的对抗无意识滑动，重建时间感知。

## 产品理念

本应用基于柳比歇夫时间记录法：每次使用手机前必须明确声明目的，通过"持续在场的视觉锚点"唤醒自我觉察，对抗习惯建立困难与使用中遗忘的问题。

## 核心功能

- ✅ **语音/键盘输入使用目的** - 快速记录本次使用手机的意图
- ✅ **正计时/倒计时选择** - 支持 1-120 分钟灵活设置
- ✅ **顶部常驻悬浮窗** - 持续显示目的文本 + 实时计时
- ✅ **暂停/结束控制** - 点击悬浮窗展开控制按钮
- ✅ **本地日志归档** - Room 数据库持久化会话记录
- ✅ **权限引导页** - 分机型权限开启指引
- ✅ **低功耗保活** - ForegroundService + START_STICKY

## 技术特性

- **纯软件方案** - 不劫持系统、不限制其他 App、不依赖网络
- **精准计时** - 基于 `SystemClock.elapsedRealtime()`，UI 刷新锁定 1Hz
- **Android 兼容** - minSdk 29 (Android 10), targetSdk 34 (Android 14)
- **现代架构** - Kotlin + Jetpack Compose + MVVM + Room + Coroutines/Flow

## 权限说明

| 权限 | 用途 |
|------|------|
| SYSTEM_ALERT_WINDOW | 显示悬浮窗 |
| POST_NOTIFICATIONS | 前台服务通知 (Android 13+) |
| FOREGROUND_SERVICE_SPECIAL_USE | 特殊用途前台服务 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 电池优化提示 |

## 权限开启指引

### 悬浮窗权限
- **MIUI/HyperOS**: 设置 → 应用设置 → 授权管理 → 悬浮窗权限
- **ColorOS**: 设置 → 应用管理 → 权限管理 → 悬浮窗
- **OriginOS**: 设置 → 应用与权限 → 权限管理 → 悬浮窗
- **OneUI**: 设置 → 应用程序 → FocusIntent → 出现在顶部

### 电池优化
设置 → 电池 → 电池优化 → 所有应用 → FocusIntent → 不优化

## 本地构建

```bash
# 克隆项目
git clone <repository-url>
cd FocusIntent

# 构建 Debug APK
./gradlew assembleDebug

# 输出路径
app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD

项目配置了 GitHub Actions 自动化流水线：

- **触发条件**: push 到 `main` 分支
- **执行步骤**: Checkout → JDK 17 → Gradle Build → 上传产物
- **产物获取**: Actions 页面下载 `FocusIntent-Debug-APK`

## 项目结构

```
app/
├── src/main/
│   ├── java/com/focusintent/app/
│   │   ├── data/           # Room DAO & Database
│   │   ├── di/             # Hilt 依赖注入
│   │   ├── model/          # 数据模型
│   │   ├── service/        # ForegroundService & Repository
│   │   └── ui/             # Compose UI & Activities
│   ├── res/                # 资源文件
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## 版本信息

- **Version**: 1.0.0
- **minSdk**: 29 (Android 10)
- **targetSdk**: 34 (Android 14)
- **Compile SDK**: 34

## License

MIT License
