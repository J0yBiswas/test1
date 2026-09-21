package com.bypassbd.otp.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pushes an OTP to the relay worker: POST {server}/v1/otp/push
 * with { phone, pairCode, otp }. No third-party HTTP library — plain
 * HttpURLConnection on the IO dispatcher keeps the app tiny and dependency-free.
 */
object Relay {

    data class Result(val ok: Boolean, val message: String)

    suspend fun push(server: String, phone: String, pairCode: String, otp: String): Result =
        withContext(Dispatchers.IO) {
            val base = server.trim().trimEnd('/')
            if (base.isEmpty()) return@withContext Result(false, "No server URL set")
            val url = if (base.endsWith("/v1/otp/push")) base else "$base/v1/otp/push"

            val payload = JSONObject()
                .put("phone", phone.filter { it.isDigit() })
                .put("pairCode", pairCode.trim())
                .put("otp", otp.filter { it.isDigit() })
                .toString()

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code in 200..299) {
                    Result(true, "Delivered")
                } else {
                    val msg = runCatching { JSONObject(text).optString("message") }.getOrDefault("")
                    Result(false, "Server $code${if (msg.isNotBlank()) ": $msg" else ""}")
                }
            } catch (e: Exception) {
                Result(false, e.message ?: "Network error")
            } finally {
                conn?.disconnect()
            }
        }
}
