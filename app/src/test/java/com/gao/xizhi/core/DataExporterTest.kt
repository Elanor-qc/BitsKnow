package com.gao.xizhi.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.data.KnowledgeCard
import com.gao.xizhi.data.LearningNote
import com.gao.xizhi.data.Message
import com.gao.xizhi.data.Module
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataExporterTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `empty db exports placeholder`() = runBlocking {
        val md = DataExporter.buildMarkdown(db)
        assertTrue(md.contains("隙知学习数据导出"))
        assertTrue(md.contains("还没有任何模块"))
    }

    @Test
    fun `module with data exports all sections in order`() = runBlocking {
        val moduleId = db.moduleDao().insert(
            Module(title = "高斯泼溅", summary = "学过基础", createdAt = 0, updatedAt = 0)
        )
        db.messageDao().insert(
            Message(moduleId = moduleId, role = Message.Role.USER, content = "什么是3DGS？", createdAt = 1)
        )
        db.messageDao().insert(
            Message(moduleId = moduleId, role = Message.Role.ASSISTANT, content = "3DGS 是……", createdAt = 2)
        )
        db.knowledgeCardDao().insert(
            KnowledgeCard(moduleId = moduleId, type = KnowledgeCard.Type.TERM, title = "3DGS", body = "一种显式表示", createdAt = 3)
        )
        db.learningNoteDao().insert(LearningNote(moduleId = moduleId, content = "已掌握基本概念", createdAt = 4))

        val md = DataExporter.buildMarkdown(db)
        assertTrue(md.contains("## 高斯泼溅"))
        assertTrue(md.contains("### 学习摘要"))
        assertTrue(md.contains("### 学习记录"))
        assertTrue(md.contains("已掌握基本概念"))
        assertTrue(md.contains("### 知识卡片"))
        assertTrue(md.contains("#### [术语] 3DGS"))
        assertTrue(md.contains("### 对话记录"))
        val userIdx = md.indexOf("**我**")
        val teacherIdx = md.indexOf("**教师**")
        assertTrue(userIdx in 0 until teacherIdx)
        assertFalse(md.contains("还没有任何模块"))
    }
}
