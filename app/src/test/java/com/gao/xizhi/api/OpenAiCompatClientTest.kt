package com.gao.xizhi.api

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OpenAiCompatClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OpenAiCompatClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OpenAiCompatClient()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun sse(vararg events: String): String =
        events.joinToString("") { "data: $it\n\n" } + "data: [DONE]\n\n"

    private fun sseResponse(body: String): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "text/event-stream")
            .setBody(body)

    private fun delta(text: String): String =
        """{"choices":[{"delta":{"content":"$text"}}]}"""

    @Test
    fun streamEmitsDeltasThenDone() = runTest {
        server.enqueue(
            sseResponse(sse(delta("你好"), delta("，"), delta("世界")))
        )
        val events = client.stream(
            messages = listOf(ChatMessage.user("hi")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        ).toList()

        assertEquals(
            listOf(
                StreamEvent.Delta("你好"),
                StreamEvent.Delta("，"),
                StreamEvent.Delta("世界"),
                StreamEvent.Done,
            ),
            events,
        )
    }

    @Test
    fun streamSkipsReasoningContentAndNullContent() = runTest {
        val reasoning = """{"choices":[{"delta":{"reasoning_content":"thinking..."}}]}"""
        val empty = """{"choices":[{"delta":{}}]}"""
        server.enqueue(sseResponse(sse(reasoning, empty, delta("ok"))))
        val events = client.stream(
            messages = listOf(ChatMessage.user("hi")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        ).toList()

        assertEquals(listOf(StreamEvent.Delta("ok"), StreamEvent.Done), events)
    }

    @Test
    fun streamHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad key"}}"""))
        val events = client.stream(
            messages = listOf(ChatMessage.user("hi")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        ).toList()

        val error = events.filterIsInstance<StreamEvent.Error>().first()
        assertTrue(error.throwable is IOException)
    }

    @Test
    fun completeReturnsContent() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"choices":[{"message":{"content":"pong"}}]}""")
        )
        val result = client.complete(
            messages = listOf(ChatMessage.user("ping")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        )
        assertEquals("pong", result.getOrThrow())
    }

    @Test
    fun completeHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = client.complete(
            messages = listOf(ChatMessage.user("hi")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun urlBuilding() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiCompatClient.buildUrl("https://api.deepseek.com"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiCompatClient.buildUrl("https://api.deepseek.com/"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiCompatClient.buildUrl("https://api.deepseek.com/v1"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiCompatClient.buildUrl("https://api.deepseek.com/v1/chat/completions"),
        )
    }

    @Test
    fun `complete with malformed 200 body does not hang and returns failure`() = runTest {
        server.enqueue(MockResponse().setBody("""{"choices": []}"""))
        val result = client.complete(
            messages = listOf(ChatMessage.user("hi")),
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            model = "m",
        )
        assertTrue(result.isFailure)
    }
}
