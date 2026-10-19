# SPEC-02: API 客户端与配置

## 目标
OpenAI 兼容的 SSE 流式聊天客户端 + 设置页（API 配置 CRUD、多套切换、连通性测试）。

## 内容
1. `ChatClient` 接口：`fun stream(messages: List<ChatMessage>, config: ApiConfig): Flow<StreamEvent>`
   - StreamEvent = Delta(text) | Done | Error(throwable)
   - OkHttp 实现：POST {baseUrl}/chat/completions，stream=true，解析 SSE（data: 行、[DONE]、JSON delta.content）
   - 部分厂商的 reasoning_content 字段忽略（不展示思维链）
2. apiKey 存储不加密（纯自用单机，不做 EncryptedSharedPreferences，spec 里记录该取舍）
3. 设置页：
   - 配置列表（name + model），点选切换 active，增删改
   - 「测试连接」按钮：发一条 1-token 请求，成功/失败提示
4. 无 active 配置时，对话入口引导去设置

## 验收
- SSE 解析用 OkHttp MockWebServer 单测（普通 delta 流、[DONE]、错误响应、网络中断）
- 模拟器真连一次用户提供的 API（手动验证）
