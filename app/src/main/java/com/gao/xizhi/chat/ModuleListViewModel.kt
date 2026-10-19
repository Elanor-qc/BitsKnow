package com.gao.xizhi.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.data.Module
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModuleListViewModel(private val db: AppDatabase) : ViewModel() {

    val modules: StateFlow<List<Module>> = db.moduleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(title: String) {
        if (title.isBlank()) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            db.moduleDao().insert(Module(title = title.trim(), createdAt = now, updatedAt = now))
        }
    }

    fun delete(moduleId: Long) {
        viewModelScope.launch {
            db.withTransaction {
                db.messageDao().deleteByModule(moduleId)
                db.knowledgeCardDao().deleteByModule(moduleId)
                db.learningNoteDao().deleteByModule(moduleId)
                db.moduleDao().deleteById(moduleId)
            }
        }
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ModuleListViewModel(db) as T
    }
}
