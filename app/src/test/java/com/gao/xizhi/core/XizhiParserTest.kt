package com.gao.xizhi.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XizhiParserTest {

    @Test
    fun `no block returns raw`() {
        val reply = "普通教学回复，没有沉淀。\n\n第二段。"
        val parsed = XizhiParser.parse(reply)
        assertEquals(reply, parsed.displayContent)
        assertNull(parsed.block)
    }

    @Test
    fun `normal block stripped and parsed`() {
        val reply = """
            KL 散度衡量两个分布的差异……讲解正文。

            ```xizhi
            {"summary": "已学 KL 散度", "cards": [{"type": "term", "title": "KL散度", "body": "衡量两个概率分布差异的度量，非对称"}], "notes": ["用户已掌握 KL 散度直觉"]}
            ```
        """.trimIndent()
        val parsed = XizhiParser.parse(reply)

        assertTrue(parsed.displayContent.contains("KL 散度衡量"))
        assertTrue(!parsed.displayContent.contains("```xizhi"))

        val block = parsed.block!!
        assertEquals("已学 KL 散度", block.summary)
        assertEquals(1, block.cards.size)
        assertEquals("KL散度", block.cards[0].title)
        assertEquals(listOf("用户已掌握 KL 散度直觉"), block.notes)
    }

    @Test
    fun `malformed json falls back to raw display`() {
        val reply = "正文。\n\n```xizhi\n{broken json}\n```"
        val parsed = XizhiParser.parse(reply)
        assertEquals(reply, parsed.displayContent)
        assertNull(parsed.block)
    }

    @Test
    fun `card body containing code fence does not truncate json`() {
        val reply = """
            讲解正文。

            ```xizhi
            {"cards": [{"type": "method", "title": "梯度累积", "body": "伪代码：\n```\nfor batch:\n    loss.backward()\n```\n即先累积再更新"}]}
            ```
        """.trimIndent()
        val parsed = XizhiParser.parse(reply)
        val block = parsed.block!!
        assertEquals("梯度累积", block.cards.single().title)
        assertTrue(block.cards.single().body.contains("for batch:"))
        assertTrue(parsed.displayContent.contains("讲解正文"))
    }

    @Test
    fun `multiple xizhi blocks degrade to raw display`() {
        val reply = """
            正文。

            ```xizhi
            {"cards": [{"type": "term", "title": "旧", "body": "旧卡片"}]}
            ```

            中间还有话。

            ```xizhi
            {"cards": [{"type": "concept", "title": "新", "body": "新卡片"}]}
            ```
        """.trimIndent()
        val parsed = XizhiParser.parse(reply)
        // greedy capture spans both blocks so JSON parse fails; protocol says only one block
        // per reply, and failing safe (raw display, no data loss) is the accepted behavior
        assertNull(parsed.block)
        assertEquals(reply, parsed.displayContent)
    }

    @Test
    fun `blank cards and notes are sanitized away`() {
        val reply = "正文。\n\n```xizhi\n{\"summary\":\"\", \"cards\": [{\"type\": \"term\", \"title\": \"\", \"body\": \"b\"}], \"notes\": [\"\", \"  \"]}\n```"
        val parsed = XizhiParser.parse(reply)
        val block = parsed.block!!
        assertNull(block.summary)
        assertTrue(block.cards.isEmpty())
        assertTrue(block.notes.isEmpty())
    }

    @Test
    fun `card type mapping`() {
        assertEquals(
            com.gao.xizhi.data.KnowledgeCard.Type.TERM,
            XizhiParser.cardType("term"),
        )
        assertEquals(
            com.gao.xizhi.data.KnowledgeCard.Type.MISCONCEPTION,
            XizhiParser.cardType("misconception"),
        )
        assertEquals(
            com.gao.xizhi.data.KnowledgeCard.Type.CONCEPT,
            XizhiParser.cardType("unknown-type"),
        )
    }

    @Test
    fun `visibleStreaming hides partial xizhi block`() {
        assertEquals("正文", XizhiParser.visibleStreaming("正文\n\n```xizhi\n{\"summ"))
        assertEquals("全文", XizhiParser.visibleStreaming("全文"))
    }
}
