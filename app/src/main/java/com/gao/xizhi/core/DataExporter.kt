package com.gao.xizhi.core

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gao.xizhi.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext

object DataExporter {

    suspend fun buildMarkdown(db: AppDatabase): String {
        val all = db.moduleDao().getAll()
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
        sb.append("# 隙知学习数据导出\n\n导出时间：$now\n")
        for (module in all) {
            sb.append("\n---\n\n## ${module.title}\n")
            if (module.summary.isNotBlank()) {
                sb.append("\n### 学习摘要\n\n${module.summary}\n")
            }
            val notes = db.learningNoteDao().byModule(module.id)
            if (notes.isNotEmpty()) {
                sb.append("\n### 学习记录\n\n")
                notes.forEach { sb.append("- ${it.content}\n") }
            }
            val cards = db.knowledgeCardDao().byModule(module.id)
            if (cards.isNotEmpty()) {
                sb.append("\n### 知识卡片\n")
                cards.forEach { card ->
                    sb.append("\n#### [${card.type.label}] ${card.title}\n\n${card.body}\n")
                }
            }
            val messages = db.messageDao().latest(module.id, limit = Int.MAX_VALUE).asReversed()
            if (messages.isNotEmpty()) {
                sb.append("\n### 对话记录\n")
                messages.forEach { msg ->
                    val who = if (msg.role == com.gao.xizhi.data.Message.Role.USER) "**我**" else "**教师**"
                    sb.append("\n$who\n\n${msg.content}\n")
                }
            }
        }
        if (all.isEmpty()) sb.append("\n（还没有任何模块）\n")
        return sb.toString()
    }

    /** Returns the display name of the written file, or an error message. */
    suspend fun exportToDownloads(context: Context, db: AppDatabase): String =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext "当前系统版本过低（需要 Android 10+），暂不支持导出"
            }
            val markdown = buildMarkdown(db)
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(Date())
            val name = "xizhi-export-$stamp.md"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext "导出失败：无法创建文件"
            try {
                resolver.openOutputStream(uri)?.use { it.write(markdown.toByteArray(Charsets.UTF_8)) }
                    ?: throw java.io.IOException("openOutputStream returned null")
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                return@withContext "导出失败：${e.message ?: e.javaClass.simpleName}"
            }
            "已导出到 Download/$name"
        }
}

private val com.gao.xizhi.data.KnowledgeCard.Type.label: String
    get() = when (this) {
        com.gao.xizhi.data.KnowledgeCard.Type.CONCEPT -> "概念"
        com.gao.xizhi.data.KnowledgeCard.Type.TERM -> "术语"
        com.gao.xizhi.data.KnowledgeCard.Type.MISCONCEPTION -> "误区"
        com.gao.xizhi.data.KnowledgeCard.Type.METHOD -> "方法"
    }
