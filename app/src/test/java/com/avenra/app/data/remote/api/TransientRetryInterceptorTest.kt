package com.avenra.app.data.remote.api

import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class TransientRetryInterceptorTest {

    private val testGetRequest = Request.Builder()
        .url("https://avenra-api.bonto.run/v1/home")
        .get()
        .build()

    private val testPostRequest = Request.Builder()
        .url("https://avenra-api.bonto.run/v1/orders")
        .post("{\"quoteId\":\"123\"}".toRequestBody("application/json".toMediaType()))
        .build()

    private fun createResponse(
        code: Int,
        message: String = "OK",
        request: Request = testGetRequest,
        bodyString: String = "{\"status\":$code}",
        contentType: String? = "application/json"
    ): Response {
        val mediaType = contentType?.toMediaType()
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(bodyString.toResponseBody(mediaType))
        if (contentType != null) {
            builder.header("Content-Type", contentType)
        }
        return builder.build()
    }

    private class FakeChain(
        private val request: Request,
        private val responses: List<() -> Response>,
        private var isCanceled: Boolean = false
    ) : Interceptor.Chain {
        var callCount = 0
            private set

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            if (isCanceled) {
                throw IOException("Canceled")
            }
            if (callCount >= responses.size) {
                throw IllegalStateException("Unexpected proceed call beyond configured responses")
            }
            val provider = responses[callCount]
            callCount++
            return provider()
        }

        override fun connection() = null
        override fun call(): Call = FakeCall(this)
        override fun connectTimeoutMillis() = 15000
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun readTimeoutMillis() = 15000
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun writeTimeoutMillis() = 15000
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

        fun cancel() {
            isCanceled = true
        }

        fun getIsCanceled() = isCanceled
    }

    private class FakeCall(private val chain: FakeChain) : Call {
        override fun request(): Request = chain.request()
        override fun execute(): Response = chain.proceed(chain.request())
        override fun enqueue(responseCallback: okhttp3.Callback) {}
        override fun cancel() { chain.cancel() }
        override fun isExecuted(): Boolean = chain.callCount > 0
        override fun isCanceled(): Boolean = chain.getIsCanceled()
        override fun clone(): Call = this
        override fun timeout(): okio.Timeout = okio.Timeout.NONE
    }

    @Test
    fun `successful 200 GET request proceeds once without retry or delay`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(testGetRequest, listOf { createResponse(200) })

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `POST requests are not retried even on transient 503 status`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testPostRequest,
            listOf { createResponse(503, "Service Unavailable", testPostRequest) }
        )

        val response = interceptor.intercept(chain)

        assertEquals(503, response.code)
        assertEquals(1, chain.callCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `POST requests are not retried on ConnectException`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testPostRequest,
            listOf { throw ConnectException("Connection refused") }
        )

        try {
            interceptor.intercept(chain)
            fail("Expected ConnectException to be thrown immediately for POST")
        } catch (e: ConnectException) {
            assertEquals("Connection refused", e.message)
            assertEquals(1, chain.callCount)
            assertTrue(delays.isEmpty())
        }
    }

    @Test
    fun `4xx client errors on GET are not retried`() {
        val testCodes = listOf(400, 401, 403, 404, 422)
        for (code in testCodes) {
            val delays = mutableListOf<Long>()
            val interceptor = TransientRetryInterceptor(
                maxRetries = 2,
                initialDelayMs = 1000L,
                sleeper = { delays.add(it) }
            )
            val chain = FakeChain(testGetRequest, listOf { createResponse(code, "Client Error") })

            val response = interceptor.intercept(chain)

            assertEquals(code, response.code)
            assertEquals(1, chain.callCount)
            assertTrue(delays.isEmpty())
        }
    }

    @Test
    fun `UnknownHostException and SSLException on GET are not retried`() {
        val nonRetryableExceptions = listOf(
            UnknownHostException("Unable to resolve host"),
            SSLException("SSL handshake aborted")
        )

        for (exception in nonRetryableExceptions) {
            val delays = mutableListOf<Long>()
            val interceptor = TransientRetryInterceptor(
                maxRetries = 2,
                initialDelayMs = 1000L,
                sleeper = { delays.add(it) }
            )
            val chain = FakeChain(testGetRequest, listOf { throw exception })

            try {
                interceptor.intercept(chain)
                fail("Expected ${exception.javaClass.simpleName} to be thrown immediately")
            } catch (e: IOException) {
                assertEquals(exception.javaClass, e.javaClass)
                assertEquals(1, chain.callCount)
                assertTrue(delays.isEmpty())
            }
        }
    }

    @Test
    fun `transient 503 error on GET retries and succeeds on second attempt`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { createResponse(503, "Service Unavailable") },
                { createResponse(200, "OK") }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
        assertEquals(1000L, delays.sum())
    }

    @Test
    fun `transient SocketTimeoutException on GET retries and succeeds on third attempt`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { throw SocketTimeoutException("connect timed out") },
                { throw ConnectException("Connection refused") },
                { createResponse(200, "OK") }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(3, chain.callCount)
        assertEquals(3000L, delays.sum()) // 1000ms + 2000ms
    }

    @Test
    fun `continuous 502 error exhausts bounded retries and returns final response`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 500L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { createResponse(502, "Bad Gateway") },
                { createResponse(502, "Bad Gateway") },
                { createResponse(502, "Bad Gateway") }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(502, response.code)
        assertEquals(3, chain.callCount) // 1 initial + 2 retries
        assertEquals(1500L, delays.sum()) // 500 + 1000
    }

    @Test
    fun `continuous ConnectException exhausts bounded retries and throws exception`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { throw ConnectException("Connection refused") },
                { throw ConnectException("Connection refused") },
                { throw ConnectException("Connection refused") }
            )
        )

        try {
            interceptor.intercept(chain)
            fail("Expected ConnectException to be thrown after exhausting retries")
        } catch (e: ConnectException) {
            assertEquals("Connection refused", e.message)
            assertEquals(3, chain.callCount)
            assertEquals(3000L, delays.sum())
        }
    }

    @Test
    fun `cancellation during backoff stops retry loop immediately`() {
        lateinit var chainRef: FakeChain
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = {
                chainRef.cancel()
            }
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { createResponse(503, "Service Unavailable") },
                { createResponse(200, "OK") }
            )
        )
        chainRef = chain

        try {
            interceptor.intercept(chain)
            fail("Expected IOException on cancellation")
        } catch (e: IOException) {
            assertEquals("Canceled", e.message)
            assertEquals(1, chain.callCount)
        }
    }

    @Test
    fun `GET request with 200 OK and text_html with HTML prefix retries and succeeds when JSON is returned`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val htmlHoldingResponse = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "<!DOCTYPE html><html><body>Waking up application...</body></html>",
            contentType = "text/html; charset=utf-8"
        )
        val jsonSuccessResponse = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "{\"banners\":[],\"categories\":[]}",
            contentType = "application/json"
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { htmlHoldingResponse },
                { jsonSuccessResponse }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
        assertEquals(1000L, delays.sum())
        assertEquals("{\"banners\":[],\"categories\":[]}", response.body?.string())
    }

    @Test
    fun `GET request with 200 OK and application_json returns immediately without retry`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val jsonSuccessResponse = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "{\"banners\":[]}",
            contentType = "application/json"
        )
        val chain = FakeChain(testGetRequest, listOf { jsonSuccessResponse })

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
        assertTrue(delays.isEmpty())
        assertEquals("{\"banners\":[]}", response.body?.string())
    }

    @Test
    fun `HTML prefix detection works even if Content-Type is missing or unexpected`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        // Body starts with whitespace and '<', Content-Type is text/plain
        val htmlWithUnexpectedContentType = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "   \n<html><body>Starting container...</body></html>",
            contentType = "text/plain"
        )
        val jsonSuccessResponse = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "{\"status\":200}",
            contentType = "application/json"
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { htmlWithUnexpectedContentType },
                { jsonSuccessResponse }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
        assertEquals(1000L, delays.sum())
    }

    @Test
    fun `legitimate text_plain response without HTML prefix is not retried`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val plainTextResponse = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "Health status: healthy",
            contentType = "text/plain"
        )
        val chain = FakeChain(testGetRequest, listOf { plainTextResponse })

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
        assertTrue(delays.isEmpty())
        assertEquals("Health status: healthy", response.body?.string())
    }

    @Test
    fun `POST request with 200 text_html holding response is never retried`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            sleeper = { delays.add(it) }
        )
        val htmlResponse = createResponse(
            code = 200,
            message = "OK",
            request = testPostRequest,
            bodyString = "<html><body>Starting...</body></html>",
            contentType = "text/html"
        )
        val chain = FakeChain(testPostRequest, listOf { htmlResponse })

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `continuous 200 HTML holding response exhausts retries and returns final response`() {
        val delays = mutableListOf<Long>()
        val interceptor = TransientRetryInterceptor(
            maxRetries = 2,
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            sleeper = { delays.add(it) }
        )
        val htmlResponse1 = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "<html><body>Waking up 1...</body></html>",
            contentType = "text/html"
        )
        val htmlResponse2 = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "<html><body>Waking up 2...</body></html>",
            contentType = "text/html"
        )
        val htmlResponse3 = createResponse(
            code = 200,
            message = "OK",
            request = testGetRequest,
            bodyString = "<html><body>Waking up 3...</body></html>",
            contentType = "text/html"
        )
        val chain = FakeChain(
            testGetRequest,
            listOf(
                { htmlResponse1 },
                { htmlResponse2 },
                { htmlResponse3 }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(3, chain.callCount) // 1 initial + 2 retries
        assertEquals(3000L, delays.sum()) // 1000ms + 2000ms
        assertEquals("<html><body>Waking up 3...</body></html>", response.body?.string())
    }
}
