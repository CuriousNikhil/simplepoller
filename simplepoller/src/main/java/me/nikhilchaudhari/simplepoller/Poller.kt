package me.nikhilchaudhari.simplepoller

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.nikhilchaudhari.simplepoller.network.DEFAULT_TIMEOUT
import me.nikhilchaudhari.simplepoller.network.RequestImpl
import me.nikhilchaudhari.simplepoller.network.Response
import me.nikhilchaudhari.simplepoller.network.ResponseImpl
import javax.net.ssl.SSLContext

class Poller private constructor(
    private val onResponse: ((response: Response) -> Unit)?,
    private val onError: ((throwable: Throwable?) -> Unit)?,
    private val onEnd: (() -> Unit)?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private var base: Long = 5000,
    private val max: Long = 60000,
    private val delayFactor: Long = 2,
    private val delay: Long = 1000,
    private val isInfinite: Boolean,
    private val method: String,
    private val url: String? = null,
    private val headers: Map<String, String?> = mapOf(),
    private val params: Map<String, String> = mapOf(),
    private val data: Any? = null,
    private val timeout: Double = DEFAULT_TIMEOUT,
    private val allowRedirects: Boolean? = null,
    private val stream: Boolean = false,
    private val sslContext: SSLContext? = null
) {

    private var isStopped = false
    private val mainScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun start() {
        mainScope.launch {
            poll().catch {
                onError?.invoke(it)
            }.collect {
                onResponse?.invoke(it)
            }
        }
    }

    private fun poll(): Flow<Response> {
        return if (isInfinite) {
            initInfinitePolling()
        } else {
            initFinitePolling()
        }
    }

    private fun initInfinitePolling(): Flow<Response> {
        return channelFlow {
            while (!isStopped) {
                send(request())
                delay(delay)
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun initFinitePolling(): Flow<Response> {
        return channelFlow {
            while (base < max) {
                send(request())
                base *= delayFactor
                delay(base)
            }
        }.flowOn(Dispatchers.Default)
    }

    fun stop() {
        isStopped = true
        mainScope.cancel("Poller stopped")
    }

    private fun request(): Response {
        if (url.isNullOrEmpty()) throw Throwable("URL should not be null")
        return ResponseImpl(
            RequestImpl(
                method, url, params, headers, data, timeout, allowRedirects, stream, sslContext
            )
        ).run {
            this.init()
            this._history.last().apply {
                this@run._history.remove(this)
            }
        }
    }


    data class Builder(
        private var onResponse: ((response: Response) -> Unit)? = null,
        private var onError: ((throwable: Throwable?) -> Unit)? = null,
        private var onEnd: (() -> Unit)? = null,
        private var dispatchcer: CoroutineDispatcher = Dispatchers.Default,
        private var base: Long = 5000,
        private var max: Long = 60000,
        private var delayFactor: Long = 2,
        private var delay: Long = 1000,
        private var isInfinite: Boolean = false,
        private var method: String = "GET",
        private var url: String? = null,
        private var headers: Map<String, String?> = mapOf(),
        private var params: Map<String, String> = mapOf(),
        private var data: Any? = null,
        private var timeout: Double = DEFAULT_TIMEOUT,
        private var allowRedirects: Boolean? = null,
        private var stream: Boolean = false,
        private var sslContext: SSLContext? = null
    ) {

        fun setDispatcher(dispatcher: CoroutineDispatcher) = apply { this.dispatchcer = dispatcher }

        fun get(
            url: String, headers: Map<String, String?> = mapOf(),
            params: Map<String, String> = mapOf(), data: Any? = null,
            timeout: Double = DEFAULT_TIMEOUT, allowRedirects: Boolean? = null,
            stream: Boolean = false, sslContext: SSLContext? = null
        ) = applier("GET", url, headers, params, data, timeout, allowRedirects, stream, sslContext)

        fun post(
            url: String, headers: Map<String, String?> = mapOf(),
            params: Map<String, String> = mapOf(), data: Any? = null,
            timeout: Double = DEFAULT_TIMEOUT, allowRedirects: Boolean? = null,
            stream: Boolean = false, sslContext: SSLContext? = null
        ) = applier("POST", url, headers, params, data, timeout, allowRedirects, stream, sslContext)

        fun onResponse(code: (response: Response) -> Unit) = apply { this.onResponse = code }

        fun onError(code: (throwable: Throwable?) -> Unit) = apply { this.onError = code }

        fun onEnd(code: () -> Unit) = apply { this.onEnd = code }

        fun setIntervals(base: Long, max: Long, delayFactor: Long, delay: Long) = apply {
            this.base = base
            this.max = max
            this.delayFactor = delayFactor
            this.delay = delay
        }

        fun setInfinitePoll(isInfinite: Boolean) = apply { this.isInfinite = isInfinite }

        fun build() = Poller(
            onResponse, onError, onEnd,
            dispatchcer, base, max, delayFactor, delay,
            isInfinite, method, url, headers, params, data, timeout, allowRedirects, stream,
            sslContext
        )

        private fun applier(
            method: String,
            url: String,
            headers: Map<String, String?>,
            params: Map<String, String>,
            data: Any?,
            timeout: Double,
            allowRedirects: Boolean?,
            stream: Boolean,
            sslContext: SSLContext?
        ) = apply {
            this.method = method
            this.url = url
            this.headers = headers
            this.params = params
            this.data = data
            this.timeout = timeout
            this.allowRedirects = allowRedirects
            this.stream = stream
            this.sslContext = sslContext
        }
    }
}