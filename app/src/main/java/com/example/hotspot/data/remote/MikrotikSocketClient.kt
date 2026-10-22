package com.example.hotspot.data.remote

import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.HotspotProfile
import com.example.hotspot.data.model.HotspotUser
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import javax.net.ssl.SSLSocketFactory

class MikrotikSocketClient {

    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    @Synchronized
    fun connect(config: ConnectionConfig, timeoutMs: Int = 6000) {
        close()
        try {
            val rawSocket = if (config.useSsl) {
                SSLSocketFactory.getDefault().createSocket()
            } else {
                Socket()
            }
            rawSocket.connect(InetSocketAddress(config.host, config.port), timeoutMs)
            rawSocket.soTimeout = timeoutMs

            socket = rawSocket
            inputStream = DataInputStream(rawSocket.getInputStream())
            outputStream = DataOutputStream(rawSocket.getOutputStream())

            login(config.username, config.password)
        } catch (e: ConnectException) {
            close()
            throw Exception("Connessione rifiutata da ${config.host}:${config.port}. Verifica in MikroTik -> IP -> Services che il servizio API o la porta ${config.port} siano attivi.")
        } catch (e: SocketTimeoutException) {
            close()
            throw Exception("Timeout di connessione verso ${config.host}:${config.port}. Se usi l'emulatore Android per connetterti al PC, usa 10.0.2.2.")
        } catch (e: UnknownHostException) {
            close()
            throw Exception("Indirizzo IP/Host '${config.host}' sconosciuto o non valido.")
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    @Synchronized
    fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false
    }

    private fun login(username: String, password: String) {
        // Try direct login (RouterOS 6.43+)
        sendSentence("/login", "=name=$username", "=password=$password")
        val response = readSentence()
        val tag = response.firstOrNull() ?: throw Exception("Nessuna risposta ricevuta dalla MikroTik")

        if (tag == "!done") {
            val retMap = parseWordsToMap(response)
            if (retMap.containsKey("ret")) {
                // Challenge response per versioni RouterOS legacy (< 6.43)
                val challengeHex = retMap["ret"] ?: ""
                val md5Resp = calculateMd5Challenge(password, challengeHex)
                sendSentence("/login", "=name=$username", "=response=00$md5Resp")
                val response2 = readSentence()
                val tag2 = response2.firstOrNull()
                if (tag2 == "!trap") {
                    val map2 = parseWordsToMap(response2)
                    throw Exception(map2["message"] ?: "Autenticazione fallita per '$username'")
                }
            }
            return
        } else if (tag == "!trap") {
            // Direct login failed with trap. Attempt 2-step challenge login
            try {
                sendSentence("/login")
                val resp1 = readSentence()
                if (resp1.firstOrNull() == "!done") {
                    val map1 = parseWordsToMap(resp1)
                    val challengeHex = map1["ret"]
                    if (!challengeHex.isNullOrBlank()) {
                        val md5Resp = calculateMd5Challenge(password, challengeHex)
                        sendSentence("/login", "=name=$username", "=response=00$md5Resp")
                        val resp2 = readSentence()
                        if (resp2.firstOrNull() == "!done") return
                    }
                }
            } catch (_: Exception) {
            }
            val retMap = parseWordsToMap(response)
            throw Exception(retMap["message"] ?: "Credenziali errate o utente non autorizzato")
        } else {
            throw Exception("Risposta socket inattesa dalla MikroTik: $tag")
        }
    }

    fun fetchUsers(): List<HotspotUser> {
        sendSentence("/ip/hotspot/user/print")
        val sentences = readAllSentencesUntilDone()
        val users = mutableListOf<HotspotUser>()

        for (sentence in sentences) {
            if (sentence.firstOrNull() == "!re") {
                val map = parseWordsToMap(sentence)
                users.add(
                    HotspotUser(
                        id = map[".id"] ?: "",
                        name = map["name"] ?: "",
                        password = map["password"] ?: "",
                        profile = map["profile"] ?: "default",
                        comment = map["comment"] ?: "",
                        disabled = map["disabled"] == "true",
                        uptime = map["uptime"] ?: "0s",
                        bytesIn = map["bytes-in"]?.toLongOrNull() ?: 0L,
                        bytesOut = map["bytes-out"]?.toLongOrNull() ?: 0L,
                        limitUptime = map["limit-uptime"] ?: "",
                        limitBytesTotal = map["limit-bytes-total"]?.toLongOrNull() ?: 0L,
                        server = map["server"] ?: "all"
                    )
                )
            }
        }
        return users
    }

    fun fetchProfiles(): List<HotspotProfile> {
        sendSentence("/ip/hotspot/user/profile/print")
        val sentences = readAllSentencesUntilDone()
        val profiles = mutableListOf<HotspotProfile>()

        for (sentence in sentences) {
            if (sentence.firstOrNull() == "!re") {
                val map = parseWordsToMap(sentence)
                profiles.add(
                    HotspotProfile(
                        id = map[".id"] ?: "",
                        name = map["name"] ?: "default",
                        sharedUsers = map["shared-users"] ?: "1",
                        rateLimit = map["rate-limit"] ?: ""
                    )
                )
            }
        }
        if (profiles.isEmpty()) {
            profiles.add(HotspotProfile(id = "*0", name = "default"))
        }
        return profiles
    }

    fun addUser(user: HotspotUser) {
        val words = mutableListOf(
            "/ip/hotspot/user/add",
            "=name=${user.name}",
            "=password=${user.password}",
            "=profile=${user.profile}",
            "=disabled=${if (user.disabled) "true" else "false"}"
        )
        if (user.comment.isNotBlank()) words.add("=comment=${user.comment}")
        if (user.server.isNotBlank()) words.add("=server=${user.server}")
        if (user.limitUptime.isNotBlank()) words.add("=limit-uptime=${user.limitUptime}")
        if (user.limitBytesTotal > 0) words.add("=limit-bytes-total=${user.limitBytesTotal}")

        sendSentence(*words.toTypedArray())
        val response = readSentence()
        if (response.firstOrNull() == "!trap") {
            val map = parseWordsToMap(response)
            throw Exception(map["message"] ?: "Impossibile creare l'utente hotspot")
        }
    }

    fun updateUser(user: HotspotUser) {
        val words = mutableListOf(
            "/ip/hotspot/user/set",
            "=.id=${user.id}",
            "=name=${user.name}",
            "=password=${user.password}",
            "=profile=${user.profile}",
            "=disabled=${if (user.disabled) "true" else "false"}"
        )
        words.add("=comment=${user.comment}")
        if (user.server.isNotBlank()) words.add("=server=${user.server}")
        if (user.limitUptime.isNotBlank()) words.add("=limit-uptime=${user.limitUptime}")
        if (user.limitBytesTotal >= 0) words.add("=limit-bytes-total=${user.limitBytesTotal}")

        sendSentence(*words.toTypedArray())
        val response = readSentence()
        if (response.firstOrNull() == "!trap") {
            val map = parseWordsToMap(response)
            throw Exception(map["message"] ?: "Impossibile aggiornare l'utente hotspot")
        }
    }

    fun deleteUser(id: String) {
        sendSentence("/ip/hotspot/user/remove", "=.id=$id")
        val response = readSentence()
        if (response.firstOrNull() == "!trap") {
            val map = parseWordsToMap(response)
            throw Exception(map["message"] ?: "Impossibile eliminare l'utente hotspot")
        }
    }

    fun setUserDisabled(id: String, disabled: Boolean) {
        sendSentence("/ip/hotspot/user/set", "=.id=$id", "=disabled=${if (disabled) "true" else "false"}")
        val response = readSentence()
        if (response.firstOrNull() == "!trap") {
            val map = parseWordsToMap(response)
            throw Exception(map["message"] ?: "Impossibile cambiare lo stato dell'utente")
        }
    }

    // Protocol Encoding/Decoding
    private fun sendSentence(vararg words: String) {
        val out = outputStream ?: throw Exception("Socket non connesso")
        for (word in words) {
            writeWord(out, word)
        }
        out.writeByte(0) // Empty word terminates sentence
        out.flush()
    }

    private fun writeWord(out: OutputStream, word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        val len = bytes.size
        when {
            len < 0x80 -> {
                out.write(len)
            }
            len < 0x4000 -> {
                out.write((0x80 or (len ushr 8)))
                out.write((len and 0xFF))
            }
            len < 0x200000 -> {
                out.write((0xC0 or (len ushr 16)))
                out.write(((len ushr 8) and 0xFF))
                out.write((len and 0xFF))
            }
            len < 0x10000000 -> {
                out.write((0xE0 or (len ushr 24)))
                out.write(((len ushr 16) and 0xFF))
                out.write(((len ushr 8) and 0xFF))
                out.write((len and 0xFF))
            }
            else -> {
                out.write(0xF0)
                out.write(((len ushr 24) and 0xFF))
                out.write(((len ushr 16) and 0xFF))
                out.write(((len ushr 8) and 0xFF))
                out.write((len and 0xFF))
            }
        }
        out.write(bytes)
    }

    private fun readSentence(): List<String> {
        val inp = inputStream ?: throw Exception("Socket non connesso")
        val sentence = mutableListOf<String>()
        while (true) {
            val word = readWord(inp) ?: break
            if (word.isEmpty()) break
            sentence.add(word)
        }
        return sentence
    }

    private fun readAllSentencesUntilDone(): List<List<String>> {
        val sentences = mutableListOf<List<String>>()
        while (true) {
            val sentence = readSentence()
            if (sentence.isEmpty()) break
            sentences.add(sentence)
            val tag = sentence.firstOrNull()
            if (tag == "!done" || tag == "!trap") {
                if (tag == "!trap") {
                    val map = parseWordsToMap(sentence)
                    throw Exception(map["message"] ?: "Errore durante la lettura dal MikroTik")
                }
                break
            }
        }
        return sentences
    }

    private fun readWord(inp: InputStream): String? {
        val b1 = inp.read()
        if (b1 == -1) return null
        val len: Int = when {
            (b1 and 0x80) == 0 -> b1
            (b1 and 0xC0) == 0x80 -> ((b1 and 0x3F) shl 8) or inp.read()
            (b1 and 0xE0) == 0xC0 -> ((b1 and 0x1F) shl 16) or (inp.read() shl 8) or inp.read()
            (b1 and 0xF0) == 0xE0 -> ((b1 and 0x0F) shl 24) or (inp.read() shl 16) or (inp.read() shl 8) or inp.read()
            (b1 and 0xF8) == 0xF0 -> {
                inp.read() // Skip 0xF0
                (inp.read() shl 24) or (inp.read() shl 16) or (inp.read() shl 8) or inp.read()
            }
            else -> 0
        }
        if (len == 0) return ""

        val buffer = ByteArray(len)
        var totalRead = 0
        while (totalRead < len) {
            val read = inp.read(buffer, totalRead, len - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(buffer, 0, totalRead, Charsets.UTF_8)
    }

    private fun parseWordsToMap(words: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (word in words) {
            if (word.startsWith("=")) {
                val idx = word.indexOf('=', 1)
                if (idx != -1) {
                    val key = word.substring(1, idx)
                    val value = word.substring(idx + 1)
                    map[key] = value
                }
            }
        }
        return map
    }

    private fun calculateMd5Challenge(password: String, challengeHex: String): String {
        val md = MessageDigest.getInstance("MD5")
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val challengeBytes = hexStringToByteArray(challengeHex)

        md.update(0)
        md.update(passBytes)
        md.update(challengeBytes)

        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
