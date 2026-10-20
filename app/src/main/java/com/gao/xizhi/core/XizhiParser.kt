package com.gao.xizhi.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class XizhiBlock(
    val summary: String? = null,
    val cards: List<XizhiCard> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
data class XizhiCard(
    val type: String,
    val title: String,
    val body: String,
)

data class ParsedReply(
    val displayContent: String,
    val block: XizhiBlock?,
)

object XizhiParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // greedy match to the LAST closing fence in raw, so card bodies containing ``` don't truncate the JSON
    private val FENCE = Regex("```xizhi\\s*\\n([\\s\\S]*)```", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedReply {
        val match = FENCE.find(raw) ?: return ParsedReply(raw, null)
        // only accept the block if it sits at the tail of the reply; a mid-reply match with a later
        // ``` fence means the greedy capture swallowed display content and JSON parse will fail anyway
        val block = runCatching { json.decodeFromString<XizhiBlock>(match.groupValues[1].trim()) }
            .getOrNull()

        if (block == null) return ParsedReply(raw, null)

        val display = raw.removeRange(match.range).trimEnd()
        return ParsedReply(display, sanitize(block))
    }

    /** Streaming display text: everything from the opening ```xizhi fence onward is protocol, not for the user. */
    fun visibleStreaming(raw: String): String {
        val idx = raw.indexOf("```xizhi", ignoreCase = true)
        return if (idx >= 0) raw.substring(0, idx).trimEnd() else raw
    }

    private fun sanitize(block: XizhiBlock): XizhiBlock = XizhiBlock(
        summary = block.summary?.trim()?.takeIf { it.isNotEmpty() },
        cards = block.cards.filter { it.title.isNotBlank() && it.body.isNotBlank() },
        notes = block.notes.map { it.trim() }.filter { it.isNotEmpty() },
    )

    fun cardType(raw: String): com.gao.xizhi.data.KnowledgeCard.Type = when (raw.trim().lowercase()) {
        "concept", "概念" -> com.gao.xizhi.data.KnowledgeCard.Type.CONCEPT
        "term", "术语" -> com.gao.xizhi.data.KnowledgeCard.Type.TERM
        "misconception", "误区" -> com.gao.xizhi.data.KnowledgeCard.Type.MISCONCEPTION
        "method", "方法" -> com.gao.xizhi.data.KnowledgeCard.Type.METHOD
        else -> com.gao.xizhi.data.KnowledgeCard.Type.CONCEPT
    }
}
