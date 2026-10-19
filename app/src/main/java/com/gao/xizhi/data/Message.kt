package com.gao.xizhi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("moduleId")],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val role: Role,
    val content: String,
    val createdAt: Long,
) {
    enum class Role { USER, ASSISTANT }
}
