# SPEC-00: 项目骨架与构建环境

## 目标
建立可编译、可在模拟器运行的空项目骨架，配好本地构建环境（JDK 17 + Android SDK + Gradle）。

## 内容
1. 环境：本地安装 JDK 17（Temurin zip 免安装）、Android cmdline-tools + SDK（platform 35、build-tools、platform-tools）。环境装在 `D:\code\proj\xizhi\.env\`（不入库）。
2. Gradle 项目：Kotlin DSL，版本目录（libs.versions.toml）管理依赖，Compose + Room + OkHttp + kotlinx.serialization 的版本先声明好。
3. 单 Activity + Compose Scaffold，底部导航占位：对话 / 复盘 / 设置 三个 tab。
4. `.gitignore`（.env/、build/、local.properties、*.apk 等）。
5. 验收：`gradlew assembleDebug` 编译通过；模拟器安装启动显示三 tab 占位界面。

## 依赖边界
后续所有 spec 在此骨架上叠加。
