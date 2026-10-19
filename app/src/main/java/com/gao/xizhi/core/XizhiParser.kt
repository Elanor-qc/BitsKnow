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

    private val FENCE = Regex("```xizhi\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedReply {
        val matches = FENCE.findAll(raw).toList()
        if (matches.isEmpty()) return ParsedReply(raw, null)

        val last = matches.last()
        val block = runCatching { json.decodeFromString<XizhiBlock>(last.groupValues[1].trim()) }
            .getOrNull()

        if (block == null) return ParsedReply(raw, null)

        val display = raw.removeRange(last.range).trimEnd()
        return ParsedReply(display, sanitize(block))
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
