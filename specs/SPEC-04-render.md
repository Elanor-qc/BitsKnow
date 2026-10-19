# SPEC-04: Markdown + LaTeX 渲染

## 目标
聊天气泡内渲染 Markdown（标题/列表/代码块/粗斜体/引用/表格）与 LaTeX 公式（$...$ 与 $$...$$）。

## 内容
1. 选型：优先纯 Compose 方案（如 `com.halilibo.compose-richtext` 或同类），LaTeX 无成熟 Compose 方案则该段降级 WebView + KaTeX（本地 assets，无网络依赖）
2. 实现 MarkdownText 组件：输入 Markdown 文本，流式期间反复重组渲染需保证不闪烁、不明显卡顿（增量文本 diff 或分块渲染，实测调优）
3. 代码块：等宽字体 + 可横向滚动 + 复制按钮
4. 深浅色主题均正常

## 验收
- 渲染测试页（debug 入口）覆盖：标题、列表、表格、行内代码、代码块、$x^2$、$$\int_0^1 f(x)dx$$、混合中英文
- 模拟器流式打字过程中渲染稳定
