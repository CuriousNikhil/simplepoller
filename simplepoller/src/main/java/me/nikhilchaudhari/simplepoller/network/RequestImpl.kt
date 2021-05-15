package me.nikhilchaudhari.simplepoller.network

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.IDN
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import javax.net.ssl.SSLContext

class RequestImpl internal constructor(
    override val method: String,
    url: String,
    override val params: Map<String, String>,
    headers: Map<String, String?>,
    data: Any?,
    override val timeout: Double,
    allowRedirects: Boolean?,
    override val stream: Boolean,
    override val sslContext: SSLContext?
) : Request {

    companion object {
        val DEFAULT_HEADERS = mapOf(
            "Accept" to "*/*",
            "Accept-Encoding" to "gzip, deflate"
        )
        val DEFAULT_DATA_HEADERS = mapOf(
            "Content-Type" to "text/plain"
        )
        val DEFAULT_FORM_HEADERS = mapOf(
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val DEFAULT_UPLOAD_HEADERS = mapOf(
            "Content-Type" to "multipart/form-data; boundary=%s"
        )
        val DEFAULT_JSON_HEADERS = mapOf(
            "Content-Type" to "application/json"
        )
    }

    // Request
    override val url: String
    override val headers: Map<String, String>
    override val data: Any?
    override val allowRedirects = allowRedirects ?: (this.method != "HEAD")

    private var _body: ByteArray? = null
    override val body: ByteArray
        get() {
            if (this._body == null) {
                val requestData = this.data
                if (requestData == null) {
                    this._body = ByteArray(0)
                    return this._body
                        ?: throw IllegalStateException("Set to null by another thread")
                }
                val data: Any? = if (requestData != null) {
                    if (requestData is Map<*, *> && requestData !is Parameters) {
                        Parameters(requestData.mapKeys { it.key.toString() }
                            .mapValues { it.value.toString() })
                    } else {
                        requestData
                    }
                } else {
                    null
                }
                if (data != null) {
                    require(data is Map<*, *>) { "data must be a Map" }
                }
                val bytes = ByteArrayOutputStream()
                if (data !is InputStream) {
                    bytes.write(data.toString().toByteArray())
                }
                this._body = bytes.toByteArray()
            }
            return this._body ?: throw IllegalStateException("Set to null by another thread")
        }


    init {
        this.url = this.makeRoute(url)
        if (URI(this.url).scheme !in setOf("http", "https")) {
            throw IllegalArgumentException("Invalid schema. Only http:// and https:// are supported.")
        }
        val mutableHeaders = CaseInsensitiveMutableMap(headers.toSortedMap())

        this.data = data
        if (data != null) {
            if (data is Map<*, *>) {
                mutableHeaders.putAllIfAbsentWithNull(DEFAULT_FORM_HEADERS)
            } else {
                mutableHeaders.putAllIfAbsentWithNull(DEFAULT_DATA_HEADERS)
            }
        }
        mutableHeaders.putAllIfAbsentWithNull(DEFAULT_HEADERS)

        val nonNullHeaders: MutableMap<String, String> =
            mutableHeaders.filterValues { it != null }.mapValues { it.value!! }.toSortedMap()

        this.headers = CaseInsensitiveMutableMap(nonNullHeaders)
    }

    private fun URL.toIDN(): URL {
        val newHost = IDN.toASCII(this.host)
        val query = this.query?.run { URLDecoder.decode(this, "UTF-8") }
        return URL(
            URI(
                this.protocol,
                this.userInfo,
                newHost,
                this.port,
                this.path,
                query,
                this.ref
            ).toASCIIString()
        )
    }

    private fun makeRoute(route: String) =
        URL(route + if (this.params.isNotEmpty()) "?${Parameters(this.params)}" else "").toIDN()
            .toString()

}

