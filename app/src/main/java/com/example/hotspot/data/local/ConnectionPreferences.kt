package com.example.hotspot.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.ConnectionType

class ConnectionPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mikrotik_hotspot_prefs", Context.MODE_PRIVATE)

    fun saveConfig(config: ConnectionConfig) {
        prefs.edit().apply {
            putString("host", config.host)
            putInt("port", config.port)
            putString("username", config.username)
            putString("password", config.password)
            putBoolean("use_ssl", config.useSsl)
            putString("connection_type", config.connectionType.name)
            apply()
        }
    }

    fun getConfig(): ConnectionConfig {
        val host = prefs.getString("host", "192.168.88.1") ?: "192.168.88.1"
        val port = prefs.getInt("port", 8278) // Default port 8278 as requested by user
        val username = prefs.getString("username", "admin") ?: "admin"
        val password = prefs.getString("password", "") ?: ""
        val useSsl = prefs.getBoolean("use_ssl", false)
        val typeStr = prefs.getString("connection_type", ConnectionType.SOCKET_API.name)
        val connectionType = try {
            ConnectionType.valueOf(typeStr ?: ConnectionType.SOCKET_API.name)
        } catch (_: Exception) {
            ConnectionType.SOCKET_API
        }

        return ConnectionConfig(
            host = host,
            port = port,
            username = username,
            password = password,
            useSsl = useSsl,
            connectionType = connectionType
        )
    }
}
