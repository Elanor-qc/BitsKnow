# 隙知 (Xizhi)

碎片化 AI 对话式学习 App（纯自用，安卓）。一个学习专题一个模块，对话即教学，
AI 边聊边沉淀知识卡片，数据全本地，可复盘。

## 技术栈

- Kotlin + Jetpack Compose（单 Activity，MVVM）
- Room（本地 SQLite）
- OkHttp + kotlinx.serialization（OpenAI 兼容 SSE 流式）
- minSdk 26 / targetSdk 35

## Spec 文档

见 [specs/](./specs/) 目录，按编号顺序实现。每个 spec 完成后跑多 agent 代码审查。

## 需求共识摘要

- 模块：一个学习专题一个模块，列表/新建/删除
- 对话：SSE 流式 + Markdown + LaTeX 渲染
- 沉淀：AI 回复末尾内嵌 JSON 块（知识卡片 + 摘要增量），App 解析入库并从气泡隐藏
- 复盘：扁平卡片流（类型：概念/术语/误区/方法），按时间倒序，类型筛选，术语表聚合
- 上下文：进度摘要(~500字) + 学习记录 + 本模块术语表 + 最近几条对话
- API：OpenAI 兼容（base URL / key / 模型名），多套配置可切换
- 不做（留口子）：联网搜索、SRS、导出备份（上线前补）、通知
