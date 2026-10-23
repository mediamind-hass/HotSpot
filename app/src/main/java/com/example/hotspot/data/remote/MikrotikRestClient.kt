package com.example.hotspot.data.remote

import android.util.Base64
import com.example.hotspot.data.model.AuthenticationException
import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.HotspotProfile
import com.example.hotspot.data.model.HotspotUser
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MikrotikRestClient {

    private var config: ConnectionConfig? = null

    fun setConfig(config: ConnectionConfig) {
        this.config = config
    }

    fun testConnection(timeoutMs: Int = 6000): Boolean {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user")
        val conn = openConnection(url, cfg, "GET", timeoutMs)
        try {
            val code = conn.responseCode
            if (code in 200..299) return true
            val errText = readStream(conn.errorStream ?: conn.inputStream)
            if (code == 401) {
                throw AuthenticationException("Credenziali errate (Username o Password non validi per MikroTik su porta ${cfg.port})")
            }
            throw Exception("HTTP $code: $errText")
        } finally {
            conn.disconnect()
        }
    }

    fun fetchUsers(): List<HotspotUser> {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user")
        val conn = openConnection(url, cfg, "GET")
        try {
            checkResponse(conn)
            val jsonText = readStream(conn.inputStream)
            val jsonArray = JSONArray(jsonText)
            val users = mutableListOf<HotspotUser>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                users.add(
                    HotspotUser(
                        id = obj.optString(".id", ""),
                        name = obj.optString("name", ""),
                        password = obj.optString("password", ""),
                        profile = obj.optString("profile", "default"),
                        comment = obj.optString("comment", ""),
                        disabled = obj.optBoolean("disabled", false) || obj.optString("disabled") == "true",
                        uptime = obj.optString("uptime", "0s"),
                        bytesIn = obj.optLong("bytes-in", 0L),
                        bytesOut = obj.optLong("bytes-out", 0L),
                        limitUptime = obj.optString("limit-uptime", ""),
                        limitBytesTotal = obj.optLong("limit-bytes-total", 0L),
                        server = obj.optString("server", "all")
                    )
                )
            }
            return users
        } finally {
            conn.disconnect()
        }
    }

    fun fetchProfiles(): List<HotspotProfile> {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user/profile")
        val conn = openConnection(url, cfg, "GET")
        try {
            checkResponse(conn)
            val jsonText = readStream(conn.inputStream)
            val jsonArray = JSONArray(jsonText)
            val profiles = mutableListOf<HotspotProfile>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                profiles.add(
                    HotspotProfile(
                        id = obj.optString(".id", ""),
                        name = obj.optString("name", "default"),
                        sharedUsers = obj.optString("shared-users", "1"),
                        rateLimit = obj.optString("rate-limit", "")
                    )
                )
            }
            if (profiles.isEmpty()) {
                profiles.add(HotspotProfile(id = "*0", name = "default"))
            }
            return profiles
        } finally {
            conn.disconnect()
        }
    }

    fun addUser(user: HotspotUser) {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user")
        val conn = openConnection(url, cfg, "PUT")

        val payload = JSONObject().apply {
            put("name", user.name)
            put("password", user.password)
            put("profile", user.profile)
            put("disabled", user.disabled.toString())
            if (user.comment.isNotBlank()) put("comment", user.comment)
            if (user.server.isNotBlank()) put("server", user.server)
            if (user.limitUptime.isNotBlank()) put("limit-uptime", user.limitUptime)
            if (user.limitBytesTotal > 0) put("limit-bytes-total", user.limitBytesTotal)
        }

        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }

        try {
            checkResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    fun updateUser(user: HotspotUser) {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val userPath = if (user.id.isNotBlank()) user.id else user.name
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user/$userPath")
        val conn = openConnection(url, cfg, "PATCH")

        val payload = JSONObject().apply {
            put("name", user.name)
            put("password", user.password)
            put("profile", user.profile)
            put("disabled", user.disabled.toString())
            put("comment", user.comment)
            if (user.server.isNotBlank()) put("server", user.server)
            if (user.limitUptime.isNotBlank()) put("limit-uptime", user.limitUptime)
            if (user.limitBytesTotal >= 0) put("limit-bytes-total", user.limitBytesTotal)
        }

        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }

        try {
            checkResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    fun deleteUser(id: String) {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user/$id")
        val conn = openConnection(url, cfg, "DELETE")
        try {
            checkResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    fun setUserDisabled(id: String, disabled: Boolean) {
        val cfg = config ?: throw Exception("Configurazione non impostata")
        val url = URL("${getProtocol(cfg)}://${cfg.host}:${cfg.port}/rest/ip/hotspot/user/$id")
        val conn = openConnection(url, cfg, "PATCH")

        val payload = JSONObject().apply {
            put("disabled", disabled.toString())
        }

        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }

        try {
            checkResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnection(url: URL, cfg: ConnectionConfig, method: String, timeoutMs: Int = 6000): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        setRequestMethod(conn, method)
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs

        val credentials = "${cfg.username}:${cfg.password}"
        val authHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        conn.setRequestProperty("Authorization", authHeader)

        if (conn is HttpsURLConnection && cfg.useSsl) {
            trustAllCertificates(conn)
        }
        return conn
    }

    private fun setRequestMethod(conn: HttpURLConnection, method: String) {
        if (method == "PATCH") {
            try {
                conn.requestMethod = "PATCH"
            } catch (_: Exception) {
                try {
                    val field = HttpURLConnection::class.java.getDeclaredField("method")
                    field.isAccessible = true
                    field.set(conn, "PATCH")
                } catch (_: Exception) {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
            }
        } else {
            conn.requestMethod = method
        }
    }

    private fun checkResponse(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code !in 200..299) {
            val errText = readStream(conn.errorStream ?: conn.inputStream)
            if (code == 401) {
                throw AuthenticationException("Credenziali errate (Username o Password non validi per MikroTik)")
            }
            val msg = try {
                val json = JSONObject(errText)
                json.optString("detail", json.optString("message", errText))
            } catch (_: Exception) {
                errText
            }
            throw Exception("Errore RouterOS REST ($code): $msg")
        }
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun getProtocol(cfg: ConnectionConfig): String = if (cfg.useSsl) "https" else "http"

    private fun trustAllCertificates(conn: HttpsURLConnection) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            conn.sslSocketFactory = sc.socketFactory
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
        } catch (_: Exception) {
        }
    }
}
