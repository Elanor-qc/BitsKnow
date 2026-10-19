package com.gao.xizhi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

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

    private fun now() = System.currentTimeMillis()

    @Test
    fun moduleCrud() = runTest {
        val dao = db.moduleDao()
        val id = dao.insert(Module(title = "3DGS", createdAt = now(), updatedAt = now()))
        dao.insert(Module(title = "VAE", createdAt = now(), updatedAt = now() + 100))

        val list = dao.observeAll().first()
        assertEquals(listOf("VAE", "3DGS"), list.map { it.title })

        val m = dao.getById(id)!!
        dao.update(m.copy(summary = "已学完基础概念"))
        assertEquals("已学完基础概念", dao.getById(id)!!.summary)

        dao.deleteById(id)
        assertNull(dao.getById(id))
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun messageLatestReturnsRecentInOrder() = runTest {
        val dao = db.messageDao()
        val moduleId = 1L
        repeat(30) { i ->
            dao.insert(Message(moduleId = moduleId, role = Message.Role.USER, content = "m$i", createdAt = now() + i))
        }
        val latest = dao.latest(moduleId, limit = 5)
        assertEquals(listOf("m29", "m28", "m27", "m26", "m25"), latest.map { it.content })
    }

    @Test
    fun knowledgeCardFilterAndDelete() = runTest {
        val dao = db.knowledgeCardDao()
        dao.insertAll(
            listOf(
                KnowledgeCard(moduleId = 1, type = KnowledgeCard.Type.TERM, title = "VAE", body = "b1", createdAt = now()),
                KnowledgeCard(moduleId = 1, type = KnowledgeCard.Type.CONCEPT, title = "KL散度", body = "b2", createdAt = now() + 1),
                KnowledgeCard(moduleId = 2, type = KnowledgeCard.Type.TERM, title = "Splat", body = "b3", createdAt = now() + 2),
            )
        )
        val module1 = dao.observeByModule(1).first()
        assertEquals(listOf("KL散度", "VAE"), module1.map { it.title })
        assertEquals(3, dao.observeAll().first().size)

        dao.deleteByModule(1)
        assertEquals(listOf("Splat"), dao.observeAll().first().map { it.title })
    }

    @Test
    fun learningNoteByModule() = runTest {
        val dao = db.learningNoteDao()
        dao.insert(LearningNote(moduleId = 1, content = "已知线性代数", createdAt = now()))
        dao.insert(LearningNote(moduleId = 1, content = "已理解 ELBO", createdAt = now() + 1))
        dao.insert(LearningNote(moduleId = 2, content = "其他模块", createdAt = now() + 2))

        val notes = dao.byModule(1)
        assertEquals(listOf("已知线性代数", "已理解 ELBO"), notes.map { it.content })
    }

    @Test
    fun apiConfigActiveSwitch() = runTest {
        val dao = db.apiConfigDao()
        val deepseek = dao.insert(ApiConfig(name = "deepseek", baseUrl = "https://api.deepseek.com", apiKey = "k", model = "deepseek-chat"))
        val kimi = dao.insert(ApiConfig(name = "kimi", baseUrl = "https://api.moonshot.cn", apiKey = "k", model = "moonshot-v1"))

        assertNull(dao.getActive())

        dao.activate(deepseek)
        assertEquals("deepseek", dao.getActive()!!.name)

        dao.activate(kimi)
        val active = dao.getActive()!!
        assertEquals("kimi", active.name)
        assertFalse(dao.getById(deepseek)!!.isActive)
        assertTrue(active.isActive)

        dao.deleteById(kimi)
        assertNull(dao.getActive())
        assertEquals(1, dao.observeAll().first().size)
    }
}
