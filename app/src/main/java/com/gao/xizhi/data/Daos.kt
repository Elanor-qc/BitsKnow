package com.gao.xizhi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Module>>

    @Query("SELECT * FROM modules ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Module>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getById(id: Long): Module?

    @Insert
    suspend fun insert(module: Module): Long

    @Update
    suspend fun update(module: Module)

    @Query("DELETE FROM modules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE moduleId = :moduleId ORDER BY id DESC LIMIT :limit")
    suspend fun latest(moduleId: Long, limit: Int): List<Message>

    @Insert
    suspend fun insert(message: Message): Long

    @Query("DELETE FROM messages WHERE moduleId = :moduleId")
    suspend fun deleteByModule(moduleId: Long)
}

@Dao
interface KnowledgeCardDao {
    @Query("SELECT * FROM knowledge_cards WHERE moduleId = :moduleId ORDER BY id DESC")
    fun observeByModule(moduleId: Long): Flow<List<KnowledgeCard>>

    @Query("SELECT * FROM knowledge_cards WHERE moduleId = :moduleId ORDER BY id ASC")
    suspend fun byModule(moduleId: Long): List<KnowledgeCard>

    @Query("SELECT * FROM knowledge_cards ORDER BY id DESC")
    fun observeAll(): Flow<List<KnowledgeCard>>

    @Insert
    suspend fun insert(card: KnowledgeCard): Long

    @Insert
    suspend fun insertAll(cards: List<KnowledgeCard>): List<Long>

    @Query("DELETE FROM knowledge_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM knowledge_cards WHERE moduleId = :moduleId")
    suspend fun deleteByModule(moduleId: Long)
}

@Dao
interface LearningNoteDao {
    @Query("SELECT * FROM learning_notes WHERE moduleId = :moduleId ORDER BY id ASC")
    suspend fun byModule(moduleId: Long): List<LearningNote>

    @Insert
    suspend fun insert(note: LearningNote): Long

    @Query("DELETE FROM learning_notes WHERE moduleId = :moduleId")
    suspend fun deleteByModule(moduleId: Long)
}

@Dao
interface ApiConfigDao {
    @Query("SELECT * FROM api_configs ORDER BY id ASC")
    fun observeAll(): Flow<List<ApiConfig>>

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ApiConfig?

    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getById(id: Long): ApiConfig?

    @Insert
    suspend fun insert(config: ApiConfig): Long

    @Update
    suspend fun update(config: ApiConfig)

    @Query("UPDATE api_configs SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE api_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @androidx.room.Transaction
    suspend fun activate(id: Long) {
        clearActive()
        setActive(id)
    }

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
