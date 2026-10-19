package com.gao.xizhi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_cards",
    indices = [Index("moduleId")],
)
data class KnowledgeCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val type: Type,
    val title: String,
    val body: String,
    val createdAt: Long,
) {
    enum class Type { CONCEPT, TERM, MISCONCEPTION, METHOD }
}
