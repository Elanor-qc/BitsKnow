package com.gao.xizhi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_notes",
    indices = [Index("moduleId")],
)
data class LearningNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val content: String,
    val createdAt: Long,
)
