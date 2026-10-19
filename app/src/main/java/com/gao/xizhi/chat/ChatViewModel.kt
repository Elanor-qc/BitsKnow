package com.gao.xizhi.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gao.xizhi.api.ChatClient
import com.gao.xizhi.api.StreamEvent
import com.gao.xizhi.api.OpenAiCompatClient
import com.gao.xizhi.core.ContextBuilder
import com.gao.xizhi.core.XizhiParser
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.data.KnowledgeCard
import com.gao.xizhi.data.Message
import com.gao.xizhi.data.MessageDao
import com.gao.xizhi.data.Module
import com.gao.xizhi.data.ModuleDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val moduleId: Long,
    private val moduleDao: ModuleDao,
    private val messageDao: MessageDao,
    private val cardDao: AppDatabase,
    private val client: ChatClient,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private val db get() = cardDao
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            messageDao.latest(moduleId, limit = 200).let { msgs ->
                _state.value = _state.value.copy(messages = msgs.reversed())
            }
        }
    }

    fun send(text: String) {
        if (_state.value.isStreaming || text.isBlank()) return
        val userMessage = Message(
            moduleId = moduleId,
            role = Message.Role.USER,
            content = text.trim(),
            createdAt = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            error = null,
        )
        streamJob = viewModelScope.launch {
            messageDao.insert(userMessage)
            touchModule()
            runStream()
        }
    }

    fun stop() {
        streamJob?.cancel()
        finalizeStreaming()
    }

    fun retry() {
        if (_state.value.isStreaming) return
        _state.value = _state.value.copy(error = null)
        streamJob = viewModelScope.launch { runStream() }
    }

    private suspend fun runStream() {
        _state.value = _state.value.copy(isStreaming = true, streamingText = "")
        val config = db.apiConfigDao().getActive()
        if (config == null) {
            _state.value = _state.value.copy(isStreaming = false, error = "未配置 API，请到「设置」添加并启用一个配置")
            return
        }

        val module = moduleDao.getById(moduleId) ?: run {
            _state.value = _state.value.copy(isStreaming = false, error = "模块不存在")
            return
        }
        val notes = db.learningNoteDao().byModule(moduleId)
        val terms = db.knowledgeCardDao().observeByModule(moduleId).first().filter { it.type == KnowledgeCard.Type.TERM }
        val requestMessages = ContextBuilder.buildMessages(
            module, notes, terms,
            _state.value.messages,
        )

        val builder = StringBuilder()
        try {
            client.stream(requestMessages, config.baseUrl, config.apiKey, config.model)
                .collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            builder.append(event.text)
                            _state.value = _state.value.copy(streamingText = builder.toString())
                        }
                        is StreamEvent.Done -> {}
                        is StreamEvent.Error -> throw event.throwable
                    }
                }
            persistReply(builder.toString())
        } catch (e: kotlinx.coroutines.CancellationException) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                persistReply(builder.toString())
            }
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isStreaming = false,
                streamingText = "",
                error = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    private suspend fun persistReply(raw: String) {
        if (raw.isBlank()) return
        val parsed = XizhiParser.parse(raw)

        messageDao.insert(
            Message(
                moduleId = moduleId,
                role = Message.Role.ASSISTANT,
                content = parsed.displayContent,
                createdAt = System.currentTimeMillis(),
            )
        )

        parsed.block?.let { block ->
            val now = System.currentTimeMillis()
            block.cards.forEach { card ->
                db.knowledgeCardDao().insert(
                    KnowledgeCard(
                        moduleId = moduleId,
                        type = XizhiParser.cardType(card.type) ?: KnowledgeCard.Type.CONCEPT,
                        title = card.title.trim(),
                        body = card.body.trim(),
                        createdAt = now,
                    )
                )
            }
            block.notes.forEach { note ->
                db.learningNoteDao().insert(
                    com.gao.xizhi.data.LearningNote(moduleId = moduleId, content = note, createdAt = now)
                )
            }
            if (block.summary != null) {
                val module = moduleDao.getById(moduleId)
                if (module != null) {
                    moduleDao.update(
                        module.copy(
                            summary = ContextBuilder.mergeSummary(module.summary, block.summary),
                            summaryUpdatedAt = now,
                        )
                    )
                }
            }
        }

        finalizeStreaming()
        _state.value = _state.value.copy(
            messages = messageDao.latest(moduleId, limit = 200).reversed(),
        )
    }

    private fun finalizeStreaming() {
        _state.value = _state.value.copy(isStreaming = false, streamingText = "")
    }

    private suspend fun touchModule() {
        moduleDao.getById(moduleId)?.let {
            moduleDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    class Factory(
        private val moduleId: Long,
        private val db: AppDatabase,
        private val client: ChatClient = OpenAiCompatClient(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(
                moduleId = moduleId,
                moduleDao = db.moduleDao(),
                messageDao = db.messageDao(),
                cardDao = db,
                client = client,
            ) as T
    }
}
