# BiliMusic

B站音乐播放器 —— 把B站视频当音乐听。

纯客户端 Android 应用，不依赖任何后端服务。使用B站视频作为音乐来源，提供类似主流音乐 App 的播放体验。

## 功能

- **扫码登录**：生成授权二维码，用B站 APP 扫码确认，自动获取 cookie
- **搜索**：关键词搜索B站视频，WBI 签名保证接口合规，搜索历史记录
- **播放**：DASH 音频流式播放，后台播放、通知栏控制、自动连播
- **沉浸式播放页**：大封面、进度拖拽、上一首/下一首、三档循环模式（顺序/列表循环/单曲循环）
- **播放队列**：BottomSheet 弹出播放队列，支持点击跳转和删除
- **乐库**：我喜欢、最近播放、B站收藏夹浏览（两级导航，登录后可用）
- **迷你播放条**：底部常驻迷你条，快捷收藏、播放/暂停、打开播放队列
- **我的**：账号信息、横向快捷入口（收藏/最近播放/收藏夹）、GitHub 仓库入口、关于
- **深色主题**：Spotify 风格深色 UI，亮青点缀

## 技术栈

| 层 | 方案 |
|---|---|
| 语言 | Java 17 |
| 播放 | Media3 ExoPlayer 1.5.1 + MediaSession |
| 网络 | HttpURLConnection + Gson（不引入 Retrofit/OkHttp） |
| 存储 | JSON 文件（不引入 Room） |
| 图片 | 自写 ImageLoader（不引入 Glide） |
| UI | Material Components，Spotify 深色风格 |
| 构建 | AGP 9.2.1 + Gradle 9.4.1 |

## 构建环境

- JDK 21（推荐 Android Studio 内置 JBR）
- Android SDK Platform `android-36.1`
- minSdk 26 / targetSdk 35
- ABI：arm64-v8a

## 构建方法

**Android Studio**：打开项目，Build → Make Project

**命令行**（需先配置 JAVA_HOME 指向 JBR 21）：

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -VerboseBuild
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/yuuuno224/bilimusic/
├── BiliMusicApp.java          # Application 入口
├── net/                        # 网络层
│   ├── BiliHttp.java           # HTTP 客户端 + Cookie 管理
│   ├── BiliApi.java            # B站 API 封装
│   ├── WbiSigner.java          # WBI 参数签名
│   ├── CookieStore.java        # Cookie 持久化
│   └── UrlCodec.java           # URL 编解码
├── model/                      # 数据模型
├── store/                      # 本地存储（Song, MusicStore）
├── auth/                       # 扫码登录（AuthManager）
├── player/                     # 播放服务
│   ├── PlaybackService.java    # MediaSessionService + ExoPlayer
│   └── PlayerConnection.java   # MediaController 封装
├── repo/                       # 数据仓库（MusicRepository）
├── ui/                         # 界面
│   ├── MainActivity.java       # 主界面 + 迷你播放条
│   ├── SearchFragment.java     # 搜索页
│   ├── LibraryFragment.java    # 乐库页（我喜欢/最近播放/收藏夹）
│   ├── MeFragment.java         # 我的页
│   ├── NowPlayingActivity.java # 沉浸式播放页
│   ├── QrLoginActivity.java    # 扫码登录页
│   ├── SongAdapter.java        # 歌曲列表适配器
│   ├── FavFolderAdapter.java   # 收藏夹适配器
│   ├── PlaylistAdapter.java    # 播放队列适配器
│   └── PlaylistDialog.java     # 播放队列弹窗
└── util/
    └── ImageLoader.java        # 图片加载器
```

## 发布

项目配置了 GitHub Actions 自动构建（`.github/workflows/build-release.yml`）。

打 tag 并推送即可触发自动构建发布：

```bash
git tag v1.0.0
git push origin v1.0.0
```

构建完成后会自动在 GitHub Releases 页面发布 APK。

## 协议

本项目仅供学习和个人使用。请遵守B站相关服务条款。
