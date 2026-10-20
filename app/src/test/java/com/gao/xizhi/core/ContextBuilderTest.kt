package com.gao.xizhi.core

import com.gao.xizhi.data.KnowledgeCard
import com.gao.xizhi.data.LearningNote
import com.gao.xizhi.data.Message
import com.gao.xizhi.data.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderTest {

    private val module = Module(id = 1, title = "3DGS", summary = "已学完全球高斯表示", createdAt = 0, updatedAt = 0)

    @Test
    fun `system prompt contains module title, summary, notes and terms`() {
        val notes = listOf(LearningNote(moduleId = 1, content = "已知线性代数", createdAt = 0))
        val terms = listOf(
            KnowledgeCard(moduleId = 1, type = KnowledgeCard.Type.TERM, title = "Splat", body = "椭球表示", createdAt = 0)
        )
        val prompt = ContextBuilder.systemPrompt(module, notes, terms)

        assertTrue(prompt.contains("3DGS"))
        assertTrue(prompt.contains("已学完全球高斯表示"))
        assertTrue(prompt.contains("已知线性代数"))
        assertTrue(prompt.contains("Splat: 椭球表示"))
        assertTrue(prompt.contains("```xizhi"))
    }

    @Test
    fun `empty context omits sections`() {
        val prompt = ContextBuilder.systemPrompt(module.copy(summary = ""), emptyList(), emptyList())
        assertFalse(prompt.contains("## 学习进度摘要"))
        assertFalse(prompt.contains("## 学习记录"))
        assertFalse(prompt.contains("## 已沉淀的术语表"))
    }

    @Test
    fun `buildMessages prepends system and maps roles`() {
        val messages = listOf(
            Message(id = 1, moduleId = 1, role = Message.Role.USER, content = "u1", createdAt = 0),
            Message(id = 2, moduleId = 1, role = Message.Role.ASSISTANT, content = "a1", createdAt = 1),
            Message(id = 3, moduleId = 1, role = Message.Role.USER, content = "u2", createdAt = 2),
        )
        val result = ContextBuilder.buildMessages(module, emptyList(), emptyList(), messages)

        assertEquals(4, result.size)
        assertEquals("system", result[0].role)
        assertEquals("u1", result[1].content)
        assertEquals("user", result[1].role)
        assertEquals("assistant", result[2].role)
        assertEquals("u2", result[3].content)
    }

    @Test
    fun `history truncated to 20`() {
        val messages = (1..30).map {
            Message(moduleId = 1, role = Message.Role.USER, content = "m$it", createdAt = it.toLong())
        }
        val result = ContextBuilder.buildMessages(module, emptyList(), emptyList(), messages)
        assertEquals(21, result.size)
        assertEquals("m11", result[1].content)
        assertEquals("m30", result.last().content)
    }

    @Test
    fun `mergeSummary appends and truncates keeping head and tail`() {
        assertEquals("new", ContextBuilder.mergeSummary("", "new"))
        assertEquals("old\nnew", ContextBuilder.mergeSummary("old", "new"))
        val merged = ContextBuilder.mergeSummary("a".repeat(790), "b".repeat(100))
        assertTrue(merged.length <= 800)
        assertTrue(merged.startsWith("a"))
        assertTrue(merged.endsWith("b"))
        assertEquals("unchanged", ContextBuilder.mergeSummary("unchanged", null))
    }
}
