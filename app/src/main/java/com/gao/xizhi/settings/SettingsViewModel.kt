package com.gao.xizhi.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gao.xizhi.api.ChatClient
import com.gao.xizhi.api.ChatMessage
import com.gao.xizhi.data.ApiConfig
import com.gao.xizhi.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val db: AppDatabase,
    private val client: ChatClient,
) : ViewModel() {

    val configs: StateFlow<List<ApiConfig>> = db.apiConfigDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult

    fun export(context: android.content.Context) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            _exportResult.value = null
            _exportResult.value = try {
                com.gao.xizhi.core.DataExporter.exportToDownloads(context.applicationContext, db)
            } catch (e: Exception) {
                "导出失败：${e.message ?: e.javaClass.simpleName}"
            }
            _exporting.value = false
        }
    }

    fun save(config: ApiConfig, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                val id = db.apiConfigDao().insert(config)
                db.apiConfigDao().activate(id)
            } else {
                db.apiConfigDao().update(config)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { db.apiConfigDao().deleteById(id) }
    }

    fun activate(id: Long) {
        viewModelScope.launch { db.apiConfigDao().activate(id) }
    }

    fun test(config: ApiConfig) {
        viewModelScope.launch {
            _testing.value = true
            _testResult.value = null
            val result = client.complete(
                messages = listOf(ChatMessage.user("回复：ok")),
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                model = config.model,
            )
            _testResult.value = result.fold(
                onSuccess = { "连接成功" },
                onFailure = { "失败：${it.message ?: it.javaClass.simpleName}" },
            )
            _testing.value = false
        }
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(db, com.gao.xizhi.api.OpenAiCompatClient()) as T
    }
}
