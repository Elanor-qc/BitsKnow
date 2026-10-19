package com.gao.xizhi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun roleToString(role: Message.Role): String = role.name

    @TypeConverter
    fun stringToRole(value: String): Message.Role = Message.Role.valueOf(value)

    @TypeConverter
    fun cardTypeToString(type: KnowledgeCard.Type): String = type.name

    @TypeConverter
    fun stringToCardType(value: String): KnowledgeCard.Type = KnowledgeCard.Type.valueOf(value)
}

@Database(
    entities = [Module::class, Message::class, KnowledgeCard::class, LearningNote::class, ApiConfig::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun messageDao(): MessageDao
    abstract fun knowledgeCardDao(): KnowledgeCardDao
    abstract fun learningNoteDao(): LearningNoteDao
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xizhi.db",
                ).build().also { instance = it }
            }
    }
}
