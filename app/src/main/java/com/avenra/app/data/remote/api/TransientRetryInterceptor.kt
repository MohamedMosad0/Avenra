package com.avenra.app.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * OkHttp interceptor that retries transient startup failures on safe (GET/HEAD) requests
 * during backend cold starts (e.g., sleeping free-tier host) using bounded exponential backoff.
 */
class TransientRetryInterceptor(
    private val maxRetries: Int = 2,
    private val initialDelayMs: Long = 1000L,
    private val backoffMultiplier: Double = 2.0,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Restrict retries to safe, idempotent requests only (GET / HEAD)
        if (!isSafeRequestMethod(request.method)) {
            return chain.proceed(request)
        }

        var attempt = 0
        var currentDelayMs = initialDelayMs

        while (true) {
            if (chain.call().isCanceled()) {
                throw IOException("Canceled")
            }

            try {
                val response = chain.proceed(request)

                if (isTransientStatusCode(response.code) && attempt < maxRetries) {
                    response.close()
                    attempt++
                    sleepBackoff(chain, currentDelayMs)
                    currentDelayMs = (currentDelayMs * backoffMultiplier).toLong()
                    continue
                }

                if (response.isSuccessful && isTransientHtmlHoldingResponse(response) && attempt < maxRetries) {
                    response.close()
                    attempt++
                    sleepBackoff(chain, currentDelayMs)
                    currentDelayMs = (currentDelayMs * backoffMultiplier).toLong()
                    continue
                }

                return response
            } catch (e: IOException) {
                val isTransient = isTransientException(e)
                val isCanceled = chain.call().isCanceled()

                if (isCanceled || !isTransient || attempt >= maxRetries) {
                    throw e
                }

                attempt++
                sleepBackoff(chain, currentDelayMs)
                currentDelayMs = (currentDelayMs * backoffMultiplier).toLong()
            }
        }
    }

    private fun isSafeRequestMethod(method: String): Boolean {
        return method.equals("GET", ignoreCase = true) || method.equals("HEAD", ignoreCase = true)
    }

    private fun isTransientStatusCode(code: Int): Boolean {
        return code == 408 || code == 502 || code == 503 || code == 504
    }

    private fun isTransientHtmlHoldingResponse(response: Response): Boolean {
        if (!isSafeRequestMethod(response.request.method)) {
            return false
        }
        val contentType = response.header("Content-Type")?.lowercase()
            ?: response.body?.contentType()?.toString()?.lowercase()
        if (contentType != null && contentType.contains("text/html")) {
            return true
        }
        val peekPrefix = runCatching {
            response.peekBody(64).string().trimStart()
        }.getOrNull()
        return peekPrefix?.startsWith("<") == true
    }

    private fun isTransientException(e: IOException): Boolean {
        return e is SocketTimeoutException ||
                e is ConnectException ||
                e.message?.contains("unexpected end of stream", ignoreCase = true) == true ||
                e.message?.contains("connection reset", ignoreCase = true) == true
    }

    private fun sleepBackoff(chain: Interceptor.Chain, delayMs: Long) {
        if (delayMs <= 0L) return
        if (chain.call().isCanceled()) {
            throw IOException("Canceled")
        }
        try {
            sleeper(delayMs)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted during retry backoff", ie)
        }
        if (chain.call().isCanceled()) {
            throw IOException("Canceled")
        }
    }
}
