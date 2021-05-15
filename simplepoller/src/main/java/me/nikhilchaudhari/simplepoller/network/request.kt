package me.nikhilchaudhari.simplepoller.network

import java.net.URLEncoder
import javax.net.ssl.SSLContext

const val DEFAULT_TIMEOUT = 30.0

interface Request {

    val method: String

    val url: String

    val params: Map<String, String>

    val headers: Map<String, String>

    val body: ByteArray

    val data: Any?

    val timeout: Double

    val allowRedirects: Boolean

    val stream: Boolean

    val sslContext: SSLContext?
}


class Parameters(private vararg val parameters: Pair<String, String>) : Map<String, String> by mapOf(*parameters) {

    constructor(parameters: Map<String, String>) : this(*parameters.toList().toTypedArray())

    override fun toString(): String {
        if (this.isEmpty()) return ""
        return buildString {
            for ((key, value) in this@Parameters) {
                if (this.isNotEmpty()) this.append("&")
                this.append(key, "=", URLEncoder.encode(value, "UTF-8"))
            }
        }
    }
}

class CaseInsensitiveMap<out V>(private val map: Map<String, V>) : Map<String, V> by map {

    override fun containsKey(key: String): Boolean {
        return this.map.keys.any { it.equals(key.toLowerCase(), ignoreCase = true) }
    }

    override fun get(key: String): V? {
        return this.map.filter { it.key.equals(key.toLowerCase(), ignoreCase = true) }.map { it.value }.firstOrNull()
    }

    override fun toString(): String {
        return this.map.toString()
    }

}


class CaseInsensitiveMutableMap<V>(private val map: MutableMap<String, V>) : MutableMap<String, V> by map {

    override fun containsKey(key: String): Boolean {
        return this.map.keys.any { it.equals(key, ignoreCase = true) }
    }

    override fun get(key: String): V? {
        return this.map.filter { it.key.equals(key, ignoreCase = true) }.map { it.value }.firstOrNull()
    }

    override fun remove(key: String): V? {
        return this.map.filter { it.key.equals(key, ignoreCase = true) }.map { it.key }.firstOrNull()?.let {
            this.map.remove(it)
        }
    }

    override fun put(key: String, value: V): V? {
        val old = this.remove(key)
        this.map[key] = value
        return old
    }

    override fun putAll(from: Map<out String, V>) {
        for ((key, value) in from) {
            this[key] = value
        }
    }

    override fun toString(): String {
        return this.map.toString()
    }
}