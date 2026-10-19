package com.gao.xizhi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modules")
data class Module(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val summaryUpdatedAt: Long = 0,
)
