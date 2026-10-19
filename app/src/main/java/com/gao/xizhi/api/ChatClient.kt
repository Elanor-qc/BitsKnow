package com.gao.xizhi.api

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
) {
    companion object {
        fun system(content: String) = ChatMessage("system", content)
        fun user(content: String) = ChatMessage("user", content)
        fun assistant(content: String) = ChatMessage("assistant", content)
    }
}

sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data object Done : StreamEvent
    data class Error(val throwable: Throwable) : StreamEvent
}

interface ChatClient {
    fun stream(
        messages: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String,
    ): kotlinx.coroutines.flow.Flow<StreamEvent>

    suspend fun complete(
        messages: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String,
        maxTokens: Int = 8,
    ): Result<String>
}
