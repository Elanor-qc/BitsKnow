package com.gao.xizhi.markdown

/**
 * Splits text into markdown and LaTeX segments.
 * $$...$$ are display math, $...$ is inline math (non-greedy, no newlines).
 */
sealed interface Segment {
    data class Markdown(val text: String) : Segment
    data class Math(val latex: String, val display: Boolean) : Segment
}

object MathSplitter {

    private val PATTERN = Regex("""\$\$([\s\S]+?)\$\$|\$([^$\n]+?)\$""")

    fun split(text: String): List<Segment> {
        if (text.isBlank()) return emptyList()
        val segments = mutableListOf<Segment>()
        var cursor = 0
        for (match in PATTERN.findAll(text)) {
            if (match.range.first > cursor) {
                segments += Segment.Markdown(text.substring(cursor, match.range.first))
            }
            val displayMath = match.groupValues[1]
            if (displayMath.isNotEmpty()) {
                segments += Segment.Math(displayMath.trim(), display = true)
            } else {
                segments += Segment.Math(match.groupValues[2].trim(), display = false)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            segments += Segment.Markdown(text.substring(cursor))
        }
        return segments
    }
}
