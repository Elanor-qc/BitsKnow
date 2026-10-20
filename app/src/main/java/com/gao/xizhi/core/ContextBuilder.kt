package com.gao.xizhi.core

import com.gao.xizhi.api.ChatMessage
import com.gao.xizhi.data.KnowledgeCard
import com.gao.xizhi.data.LearningNote
import com.gao.xizhi.data.Message
import com.gao.xizhi.data.Module

object ContextBuilder {

    private const val RECENT_MESSAGE_COUNT = 20
    private const val MAX_SUMMARY_LENGTH = 800

    fun systemPrompt(module: Module, notes: List<LearningNote>, terms: List<KnowledgeCard>): String {
        val sb = StringBuilder()
        sb.append(
            """你是「隙知」App 里的私人教师，用户正在碎片化时间学习「${module.title}」。

## 教学方式
- 移动端对话，回复保持精炼，一次只聚焦一个知识点，讲透它
- 根据下方学习记录判断用户已掌握的内容，不要重复教
- 用户提问跑偏太远时，温和拉回主线
- 中文回答；代码和术语保留英文；数学公式用 LaTeX（行内 $...$，独立 $$...$$）
"""
        )
        if (module.summary.isNotBlank()) {
            sb.append("\n## 学习进度摘要\n${module.summary.take(MAX_SUMMARY_LENGTH)}\n")
        }
        if (notes.isNotEmpty()) {
            sb.append("\n## 学习记录（按时间正序，最近的在最后）\n")
            notes.takeLast(30).forEach { sb.append("- ${it.content}\n") }
        }
        if (terms.isNotEmpty()) {
            sb.append("\n## 已沉淀的术语表（讲解时保持定义一致）\n")
            terms.forEach { sb.append("- ${it.title}: ${it.body.replace('\n', ' ')}\n") }
        }
        sb.append(
            """

## 知识沉淀协议
当本轮对话形成了一个值得长期保存的知识点（讲清一个概念、定义一个术语、纠正一个误区、总结一个方法）时，
在你回复的最末尾追加一个 ```xizhi 代码块，格式如下（严格遵守 JSON 语法，不要输出任何解释）：

```xizhi
{"summary": "进度摘要的增量改写（可选，一两句话，说明本次学到了什么、下一步该学什么）",
 "cards": [{"type": "term|concept|misconception|method", "title": "简短标题", "body": "Markdown 格式的知识卡片正文，自包含、供日后单独复习"}],
 "notes": ["用户已掌握/已纠正的一句话记录，供后续判断不重教"]}
```

规则：
- 只有真正沉淀新知识时才输出该块，普通追问、闲聊、过渡性回复不要输出
- 每张卡片必须自包含：日后单独读到它时不依赖本次对话上下文也能看懂
- 术语卡片：一旦沉淀，后续讲解使用相同定义；定义需要修正时输出新的同名卡片覆盖
- summary 是对模块整体进度的改写式描述（不是流水账），没有明显进展可省略
- 没有任何可沉淀内容时，省略整个代码块"""
        )
        return sb.toString()
    }

    fun buildMessages(
        module: Module,
        notes: List<LearningNote>,
        terms: List<KnowledgeCard>,
        recentMessages: List<Message>,
    ): List<ChatMessage> {
        val system = ChatMessage.system(systemPrompt(module, notes, terms))
        val history = recentMessages.takeLast(RECENT_MESSAGE_COUNT).map {
            if (it.role == Message.Role.USER) ChatMessage.user(it.content) else ChatMessage.assistant(it.content)
        }
        return listOf(system) + history
    }

    fun mergeSummary(current: String, increment: String?): String {
        if (increment.isNullOrBlank()) return current
        val merged = if (current.isBlank()) increment.trim()
        else current.trim() + "\n" + increment.trim()
        if (merged.length <= MAX_SUMMARY_LENGTH) return merged
        // keep head + tail so old progress isn't erased and the newest increment survives
        val keep = (MAX_SUMMARY_LENGTH - 3) / 2
        return merged.take(keep) + "\n…\n" + merged.takeLast(keep)
    }
}
