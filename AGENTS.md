# AGENTS.md — 隙知 (Xizhi) 交接文档

纯自用的碎片化 AI 对话式学习安卓 App。一个学习专题一个模块，对话即教学，
AI 回复末尾内嵌 ```xizhi JSON 块沉淀知识卡片（概念/术语/误区/方法），Room 本地存储，可复盘。
需求背景详见 README.md 和 specs/。

## 构建环境（重要，先读）

所有工具链都在项目内 `.env/`（gitignored），**不依赖系统 JDK/SDK**：

- JDK 17: `.env/jdk/jdk17`（`gradlew` 自动探测使用）
- Android SDK 35: `.env/sdk`（`local.properties` 已指向）
- 签名: `.env/xizhi-release.keystore` + `.env/keystore.properties`（storePassword/keyPassword=xizhi2026, alias=xizhi）
- Gradle 走阿里云镜像（腾讯镜像 HTTPS 不通）；JDK 走清华 Adoptium 镜像

常用命令（**必须从项目根目录跑** `./gradlew`，shell cwd 每条命令会重置回 `D:\code\proj`）：

```bash
cd /d/code/proj/xizhi
./gradlew testDebugUnitTest          # 单元测试（~41 个，含 Robolectric）
./gradlew assembleRelease            # 出签名 release APK
.env/sdk/build-tools/35.0.0/apksigner.bat verify --print-certs \
  app/build/outputs/apk/release/app-release.apk   # 验签（需先 export JAVA_HOME=.env/jdk/jdk17）
```

签名验证要看 `CN=Xizhi`；如果看到 `CN=Android Debug` 说明签名配置没生效（曾发生过，见下）。

模拟器（Windows）：

```bash
ADB=".env/sdk/platform-tools/adb.exe"
$ADB devices
$ADB install -r app/build/outputs/apk/release/app-release.apk
# 真机 UI 验证用 uiautomator dump（无障碍树，可信），不要只看截图
.env/sdk/build-tools/35.0.0/uiautomator.bat dump && adb pull //sdcard/window_dump.xml .
```

## 技术栈与架构

Kotlin 2.1.0 + Jetpack Compose (Material 3)，单 Activity + Navigation Compose，MVVM。
Room 2.6.1，OkHttp + okhttp-sse（手写 SSE 解析），kotlinx.serialization。
minSdk 26 / targetSdk 35。第三方 Markdown 渲染：Mike Penz multiplatform-markdown-renderer-m3 0.26.0。

代码分 7 个包，每个包职责单一：

| 路径 | 职责 |
|---|---|
| `api/` | OpenAiCompatClient：OpenAI 兼容 SSE 流式客户端；`extractDelta` 处理 delta 解析（**含 reasoning 模型 `content: null` 的处理，勿删**） |
| `chat/` | 模块列表 + 聊天室 UI/VM；ChatViewModel 负责 SSE 流式接收、入库、进度摘要更新 |
| `core/` | ContextBuilder（系统提示词 + 上下文组装）、XizhiParser（提取/隐藏 ```xizhi 块，`visibleStreaming` 在流式中截断 JSON 块）、DataExporter（MediaStore 导出到 Download） |
| `data/` | Room 实体（Module/Message/KnowledgeCard/LearningNote/ApiConfig）、DAO、Database |
| `markdown/` | MarkdownText（入口组件）、MathSplitter（数学/正文分割）、RenderTestDialog（渲染自测，含各种疑难用例） |
| `review/` | 复盘页：扁平卡片流 + 类型筛选 + 术语表 |
| `settings/` | API 配置管理（多套可切换）+ 导出按钮 |

### 核心机制：xizhi 协议

ContextBuilder 生成系统提示词，要求 AI 回复末尾附 ```xizhi JSON 块（summary/cards/notes）。
XizhiParser 解析入库、从聊天气泡隐藏。上下文 = 进度摘要(~500字，超 800 字截断保留头尾) + 学习记录 + 本模块术语表 + 最近对话。

### 核心机制：数学渲染管线

MathSplitter 按 `$$...$$` `$...$` `\(...\)` `\[...\]` 分割（```math/```latex fence 先归一化为 `\[...\]`；
代码 fence 内不分割；**未闭合 fence 保护其后全部内容**——这也保护流式场景）→ 正文走 Mike Penz Markdown，
公式段各起一个 WebView 加载 assets 里的 KaTeX 0.16.11（已裁剪到 608K，只留 woff2 字体）。

## 历史坑（改相关代码前必读）

1. **KaTeX base URL**：`loadDataWithBaseURL` 的 base 必须是 `file:///android_asset/`，
   HTML 内引用写 `katex/katex.min.js`（已含前缀）。曾写成 `.../katex/` 导致路径双重拼接、
   KaTeX 从未加载、静默降级纯文本——用户真机报的"LaTeX 渲染失败"就是这个，很难查。
2. **验证 UI 要用无障碍树**（uiautomator dump）：渲染成功的公式会出现真实字形节点（𝐲、⊤、矩阵括号），
   截图/图像描述会被"长得像公式的纯文本"骗过（曾因此误判"已验证渲染"）。
3. **reasoning 模型 null delta**：deepseek-reasoner 等流式时先发 `content: null`，
   `JsonNull.jsonPrimitive.content` 会返回字面字符串 "null"——必须在 OpenAiCompatClient.extractDelta 里判 JsonNull。
4. **Gradle Kotlin DSL 求值顺序**：`app/build.gradle.kts` 里 `signingConfigs {}` 块必须在 `buildTypes {}` 之前，
   否则 `signingConfigs.findByName("release")` 静默返回 null → 静默用 debug key 签名。
5. **模型无视定界符指令**：提示词要求只用 `$...$`/`$$...$$`，但模型经常输出 `\(...\)`、```math fence，
   所以 MathSplitter 必须兜底支持全部定界符，别以为提示词能管住模型。
6. **流式渲染性能**：segments 必须 `remember(text)`，WebView update 用 tag 守卫避免重复 load，
   `onRelease` 里 destroy——否则流式期间每帧重建全部 WebView（SPEC-04 审查发现的 Critical）。
7. 模拟器怪癖：adb 不在 PATH 用全路径；MSYS 下 `adb pull /sdcard/...` 会被改路径，写成 `//sdcard/...`；
   swiftshader 模拟器上快速滑动 + 多 WebView 初始化会 ANR（非代码 bug，真机无此问题）。
8. 签名不同的包无法覆盖安装（INSTALL_FAILED_UPDATE_INCOMPATIBLE），先 uninstall 再 install。

## 工作流程约定

- 需求变更 → 新增/修改 specs/SPEC-XX.md → 实现 → **每个 spec 完成后跑多 agent 并行代码审查**（用户明确要求，
  已执行的每次审查都发现了 Critical/High 级问题，不要跳过）→ 修复 → `testDebugUnitTest` + `assembleRelease` → 交付 APK 到项目根（`隙知-vX.Y.Z-release.apk`）。
- 提交信息用英文 conventional commits（见 git log）。
- Kotlin raw string（`"""..."""`）里写 `\(` 就是单反斜杠，不要写 `\\(`（曾踩坑）。

## 当前状态（2026-10-20）

- SPEC-00~07 全部完成，各经一轮以上多 agent 审查修复。
- 远端：`origin` = https://github.com/Elanor-qc/BitsKnow.git（main 分支），Release 页放 APK；
  push 走 GCM 存储凭据，GitHub API（建 Release/传附件）用 `git credential fill` 取同一 token，无需 gh CLI。
- 已有自定义花朵 logo 启动图标（mipmap-mdpi~xxxhdpi，原图见对话附件）；versionName 0.1.2 / versionCode 2。
- 最新交付：`隙知-v0.1.2-release.apk`（项目根，也可从 GitHub Release 下载），修复真机 v0.1.0 试用的三个问题：
  null-delta 输出、KaTeX 路径 bug（公式从未渲染过）、一级标题过大（提示词已禁止 `#`，标题字号已压缩）。
- 升级路径：v0.1.1 可覆盖安装 v0.1.2；v0.1.0 因签名修复需先卸载。
- 待办/已知问题：
  - 等用户真机测 v0.1.2：公式渲染是否正常、开头 null 是否消失
  - 明确不做（留口子）：联网搜索、SRS、通知；导出备份已做（MediaStore → Download）
  - API key 明文存 Room：记录在案的取舍，用户自用单机接受，不要"顺手修"
  - `.env/` 里有大量调试残留截图（shot_*.png）和 ui dump XML，可清理但别提交
