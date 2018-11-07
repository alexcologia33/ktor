package io.ktor.client.tests.mock

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.response.readText
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlin.test.*

class MockEngineTests {

    @Test
    fun testClientMock() = runBlocking {
        val mockEngine = MockEngine {
            if (url.encodedPath == "/") MockHttpResponse(
                call,
                HttpStatusCode.OK,
                ByteReadChannel(byteArrayOf(1, 2, 3)),
                headersOf("X-MyHeader", "MyValue")
            ) else MockHttpResponse(
                call, HttpStatusCode.NotFound, ByteReadChannel("Not Found ${url.encodedPath}")
            )
        }

        val client = HttpClient(mockEngine) {
            expectSuccess = false
        }

        assertEquals(byteArrayOf(1, 2, 3).toList(), client.get<ByteArray>("/").toList())
        assertEquals("MyValue", client.call("/").response.headers["X-MyHeader"])
        assertEquals("Not Found other/path", client.get<String>("/other/path"))
    }

    @Test
    fun testBasic() = testBlocking {
        val client = HttpClient(MockEngine {
            if (url.toString().endsWith("/fail")) {
                responseError(HttpStatusCode.BadRequest)
            } else {
                responseOk("$url")
            }
        })

        client.call { url("http://127.0.0.1/normal-request") }.apply {
            assertEquals("http://127.0.0.1/normal-request", response.readText())
            assertEquals(HttpStatusCode.OK, response.status)
        }

        client.call { url("http://127.0.0.1/fail") }.apply {
            assertEquals("Bad Request", response.readText())
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    private fun testBlocking(callback: suspend () -> Unit): Unit = run { runBlocking { callback() } }

}
