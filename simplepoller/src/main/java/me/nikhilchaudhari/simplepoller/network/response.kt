package me.nikhilchaudhari.simplepoller.network

import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.Charset

interface Response {
    val request: Request

    val statusCode: Int

    val headers: Map<String, String>

    val raw: InputStream

    val content: ByteArray

    val text: String

    val url: String

    var encoding: Charset

    val history: List<Response>

    val connection: HttpURLConnection

    fun contentIterator(chunkSize: Int = 1): Iterator<ByteArray>

    fun lineIterator(chunkSize: Int = 512, delimiter: ByteArray? = null): Iterator<ByteArray>

}