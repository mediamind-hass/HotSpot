package com.example.hotspot.data.model

class AuthenticationException(message: String) : Exception(message)

enum class ConnectionType {
    AUTO,
    SOCKET_API,
    REST_API
}

data class ConnectionConfig(
    val host: String = "192.168.88.1",
    val port: Int = 8278,
    val username: String = "admin",
    val password: String = "",
    val useSsl: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.AUTO
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(
        val host: String,
        val port: Int,
        val activeProtocol: String = ""
    ) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class HotspotUser(
    val id: String = "",
    val name: String = "",
    val password: String = "",
    val profile: String = "default",
    val comment: String = "",
    val disabled: Boolean = false,
    val uptime: String = "0s",
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val limitUptime: String = "",
    val limitBytesTotal: Long = 0L,
    val server: String = "all"
)

data class HotspotProfile(
    val id: String = "",
    val name: String = "default",
    val sharedUsers: String = "1",
    val rateLimit: String = ""
)
