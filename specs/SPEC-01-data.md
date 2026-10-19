# SPEC-01: 数据层

## 目标
Room 数据库与全部实体、DAO，覆盖三个核心概念的持久化。

## 实体

### Module（学习模块）
- id (PK, 自增), title, createdAt, updatedAt
- summary TEXT — 给 AI 看的进度摘要（~500字，会话结束时更新）
- summaryUpdatedAt

### Message（对话消息）
- id (PK), moduleId (FK, 索引), role (user/assistant), content
- createdAt
- 卡片 JSON 块入库前已被剥离，content 只存给用户看的正文

### KnowledgeCard（知识卡片）
- id (PK), moduleId (FK, 索引), type (concept/term/misconception/method)
- title, body (Markdown), createdAt

### LearningNote（学习记录，给 AI 上下文用）
- id (PK), moduleId (FK, 索引), content (1-3 句：掌握了什么/纠错了什么误区/已有基础), createdAt

### ApiConfig（API 配置）
- id (PK), name, baseUrl, apiKey, model, isActive (同一时刻仅一条 active)

## DAO 要求
- Module: 列表(按 updatedAt 倒序)、增删改、按 id 查
- Message: 按 moduleId 查最近 N 条（倒序取 N 再正序返回）、插入
- KnowledgeCard: 按 moduleId 倒序列表、按类型筛选、插入、删除
- LearningNote: 按 moduleId 查全部、插入
- ApiConfig: CRUD + active 切换

## 验收
单元测试（Room in-memory）覆盖各 DAO 主要路径。
