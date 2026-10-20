package com.gao.xizhi.markdown

/**
 * Splits text into markdown and LaTeX segments.
 * Supported delimiters: $$...$$ and \[...\] (display), $...$ and \(...\) (inline).
 * $ inside fenced code blocks (``` / ~~~) or escaped as \$ is left alone.
 */
sealed interface Segment {
    data class Markdown(val text: String) : Segment
    data class Math(val latex: String, val display: Boolean) : Segment
}

object MathSplitter {

    private val PATTERN = Regex(
        """(?<!\\)\$\$([\s\S]+?)(?<!\\)\$\$|(?<!\\)\$([^$\n]+?)(?<!\\)\$|\\\(([\s\S]+?)\\\)|\\\[([\s\S]+?)\\\]"""
    )
    // models often wrap display math in a ```math / ```latex fence instead of $$...$$
    private val MATH_FENCE = Regex("""```(?:math|latex)\s*\n([\s\S]*?)```""")
    private val FENCE = Regex("""^(```|~~~)""", RegexOption.MULTILINE)

    fun split(text: String): List<Segment> {
        if (text.isBlank()) return emptyList()
        val normalized = MATH_FENCE.replace(text) { "\\[" + it.groupValues[1] + "\\]" }
        val fenceSpans = findFenceSpans(normalized)
        val segments = mutableListOf<Segment>()
        var cursor = 0
        for (match in PATTERN.findAll(normalized)) {
            if (match.range.first inSpans fenceSpans) continue
            if (match.range.first > cursor) {
                segments += Segment.Markdown(normalized.substring(cursor, match.range.first))
            }
            val groups = match.groupValues
            val math = when {
                groups[1].isNotEmpty() -> Segment.Math(groups[1].trim(), display = true)
                groups[2].isNotEmpty() -> Segment.Math(groups[2].trim(), display = false)
                groups[3].isNotEmpty() -> Segment.Math(groups[3].trim(), display = false)
                else -> Segment.Math(groups[4].trim(), display = true)
            }
            if (math.latex.isEmpty()) {
                segments += Segment.Markdown(match.value)
            } else {
                segments += math
            }
            cursor = match.range.last + 1
        }
        if (cursor < normalized.length) {
            segments += Segment.Markdown(normalized.substring(cursor))
        }
        return segments
    }

    /**
     * Index ranges covering fenced code blocks (``` or ~~~).
     * An unclosed fence protects the remainder of the text, which also keeps
     * partially-streamed blocks from being math-split mid-stream.
     */
    private fun findFenceSpans(text: String): List<IntRange> {
        val spans = mutableListOf<IntRange>()
        var fenceMarker: String? = null
        var spanStart = -1
        var pos = 0
        for (line in text.split('\n')) {
            val trimmed = line.trimStart()
            val current = fenceMarker
            if (current == null) {
                val marker = FENCE.find(trimmed)?.value
                if (marker != null) {
                    fenceMarker = marker
                    spanStart = pos + (line.length - trimmed.length)
                }
            } else if (trimmed.startsWith(current)) {
                spans += spanStart until (pos + line.length)
                fenceMarker = null
            }
            pos += line.length + 1
        }
        if (fenceMarker != null && spanStart >= 0) spans += spanStart until text.length
        return spans
    }

    private infix fun Int.inSpans(spans: List<IntRange>): Boolean = spans.any { this in it }
}
