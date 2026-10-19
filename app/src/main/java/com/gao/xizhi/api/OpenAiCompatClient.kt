package com.gao.xizhi.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val json = Json { ignoreUnknownKeys = true }

class OpenAiCompatClient(
    private val client: OkHttpClient = defaultClient(),
) : ChatClient {

    override fun stream(
        messages: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String,
    ): Flow<StreamEvent> = callbackFlow {
        val url = buildUrl(baseUrl)
        val payload = buildPayload(messages, model, stream = true)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val source = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                when {
                    data == "[DONE]" -> {
                        trySend(StreamEvent.Done)
                        close()
                    }
                    else -> extractDelta(data)?.let { if (it.isNotEmpty()) trySend(StreamEvent.Delta(it)) }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val error = t ?: IOException("HTTP ${response?.code}: ${response?.message ?: "unknown error"}")
                trySend(StreamEvent.Error(error))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { source.cancel() }
    }.flowOn(Dispatchers.IO)

    override suspend fun complete(
        messages: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String,
        maxTokens: Int,
    ): Result<String> = suspendCancellableCoroutine { cont ->
        val payload = buildPayload(messages, model, stream = false, maxTokens = maxTokens)
        val request = Request.Builder()
            .url(buildUrl(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        cont.resume(Result.failure(IOException("HTTP ${it.code}: ${it.body?.string()?.take(300)}")))
                        return
                    }
                    val body = it.body?.string().orEmpty()
                    val content: String = runCatching {
                        json.parseToJsonElement(body)
                            .jsonObject["choices"]!!
                            .jsonArray[0]
                            .jsonObject["message"]!!
                            .jsonObject["content"]!!
                            .jsonPrimitive.content
                    }.getOrElse { e -> throw IOException("unexpected response: ${body.take(300)}", e) }
                    cont.resume(Result.success(content))
                }
            }
        })
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build()

        internal fun buildUrl(baseUrl: String): String {
            val base = baseUrl.trim().trimEnd('/')
            return if (base.endsWith("/chat/completions")) base else "$base/v1/chat/completions"
        }

        internal fun buildPayload(messages: List<ChatMessage>, model: String, stream: Boolean, maxTokens: Int? = null): String {
            val msgs = messages.joinToString(",") { m ->
                """{"role":"${m.role}","content":${escapeJson(m.content)}}"""
            }
            val maxTokensPart = maxTokens?.let { ""","max_tokens":$it""" } ?: ""
            return """{"model":"$model","messages":[$msgs],"stream":$stream$maxTokensPart}"""
        }

        internal fun escapeJson(s: String): String {
            val sb = StringBuilder("\"")
            for (c in s) {
                when (c) {
                    '\\' -> sb.append("\\\\")
                    '"' -> sb.append("\\\"")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '' -> sb.append("\\f")
                    else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            return sb.append("\"").toString()
        }

        internal fun extractDelta(data: String): String? = runCatching {
            val delta = json.parseToJsonElement(data)
                .jsonObject["choices"]!!
                .jsonArray[0]
                .jsonObject["delta"]!!
                .jsonObject["content"]
            delta?.jsonPrimitive?.content
        }.getOrNull()
    }
}
