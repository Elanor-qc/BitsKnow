package com.gao.xizhi.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.data.KnowledgeCard
import com.gao.xizhi.data.Module
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val moduleId: Long? = null,
    val typeFilter: KnowledgeCard.Type? = null,
    val search: String = "",
)

class ReviewViewModel(private val db: AppDatabase) : ViewModel() {

    private val _ui = MutableStateFlow(ReviewUiState())
    val ui: StateFlow<ReviewUiState> = _ui

    val modules: StateFlow<List<Module>> = db.moduleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cards: StateFlow<List<KnowledgeCard>> =
        combine(_ui, db.knowledgeCardDao().observeAll()) { ui, all ->
            all.filter { card ->
                (ui.moduleId == null || card.moduleId == ui.moduleId) &&
                    (ui.typeFilter == null || card.type == ui.typeFilter) &&
                    (ui.search.isBlank() ||
                        card.title.contains(ui.search, ignoreCase = true) ||
                        card.body.contains(ui.search, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectModule(id: Long?) {
        _ui.value = _ui.value.copy(moduleId = id)
    }

    fun setTypeFilter(type: KnowledgeCard.Type?) {
        _ui.value = _ui.value.copy(typeFilter = type)
    }

    fun setSearch(query: String) {
        _ui.value = _ui.value.copy(search = query)
    }

    fun deleteCard(id: Long) {
        viewModelScope.launch { db.knowledgeCardDao().deleteById(id) }
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel(db) as T
    }
}
