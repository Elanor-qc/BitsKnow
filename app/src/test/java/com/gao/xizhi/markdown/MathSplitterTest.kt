package com.gao.xizhi.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathSplitterTest {

    @Test
    fun `plain text is one markdown segment`() {
        val result = MathSplitter.split("普通文本，无公式。")
        assertEquals(1, result.size)
        assertEquals("普通文本，无公式。", (result[0] as Segment.Markdown).text)
    }

    @Test
    fun `inline math splits into three segments`() {
        val result = MathSplitter.split("高斯函数 \$f(x)=x^2\$ 是基础")
        assertEquals(3, result.size)
        assertEquals("高斯函数 ", (result[0] as Segment.Markdown).text)
        assertEquals("f(x)=x^2", (result[1] as Segment.Math).latex)
        assertEquals(false, (result[1] as Segment.Math).display)
        assertEquals(" 是基础", (result[2] as Segment.Markdown).text)
    }

    @Test
    fun `display math detected`() {
        val input = "公式：\n" + '$' + '$' + "\\int_0^1 f(x)dx" + '$' + '$' + "\n完毕"
        val result = MathSplitter.split(input)
        assertEquals(3, result.size)
        val math = result[1] as Segment.Math
        assertTrue(math.display)
        assertEquals("\\int_0^1 f(x)dx", math.latex)
    }

    @Test
    fun `stray dollars treated as math, katex falls back to raw text`() {
        // inherent ambiguity of $...$ delimiters; KaTeX render failure degrades to raw text, no data loss
        val input = "价格是 100" + '$' + " 到 200" + '$' + " 之间"
        val result = MathSplitter.split(input)
        assertEquals(3, result.size)
        assertEquals("到 200", (result[1] as Segment.Math).latex)
    }

    @Test
    fun `multiple formulas`() {
        val result = MathSplitter.split("\$a\$ 中间 \$b\$")
        assertEquals(3, result.size)
        assertEquals("a", (result[0] as Segment.Math).latex)
        assertEquals(" 中间 ", (result[1] as Segment.Markdown).text)
        assertEquals("b", (result[2] as Segment.Math).latex)
    }

    @Test
    fun `empty text returns empty`() {
        assertTrue(MathSplitter.split("").isEmpty())
    }

    @Test
    fun `dollars inside fenced code block are not split`() {
        val input = "代码如下：\n" + "```" + "\necho \$HOME and \$PATH\n" + "```" + "\n结尾"
        val result = MathSplitter.split(input)
        assertTrue(result.all { it is Segment.Markdown })
        assertEquals(1, result.size)
    }

    @Test
    fun `math outside fence still splits when a fence exists elsewhere`() {
        val input = "\$E=mc^2\$\n```\ncode \$x\$\n```\n"
        val result = MathSplitter.split(input)
        assertTrue(result.any { it is Segment.Math })
        assertEquals(1, result.count { it is Segment.Math })
    }

    @Test
    fun `unclosed fence protects the remainder`() {
        val input = "text\n" + "```" + "\npartial \$a\$"
        val result = MathSplitter.split(input)
        assertTrue(result.all { it is Segment.Markdown })
    }

    @Test
    fun `escaped dollar is not a math delimiter`() {
        val input = "价格 \\$5 到 \\$10 之间"
        val result = MathSplitter.split(input)
        assertEquals(1, result.size)
        assertTrue(result[0] is Segment.Markdown)
    }

    @Test
    fun `whitespace-only math is skipped`() {
        val result = MathSplitter.split("a \$ \$ b")
        assertTrue(result.none { it is Segment.Math })
    }
}
