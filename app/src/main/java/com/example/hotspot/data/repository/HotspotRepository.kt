package com.example.hotspot.data.repository

import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.ConnectionType
import com.example.hotspot.data.model.HotspotProfile
import com.example.hotspot.data.model.HotspotUser
import com.example.hotspot.data.remote.MikrotikRestClient
import com.example.hotspot.data.remote.MikrotikSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HotspotRepository(
    private val socketClient: MikrotikSocketClient = MikrotikSocketClient(),
    private val restClient: MikrotikRestClient = MikrotikRestClient()
) {

    private var currentConfig: ConnectionConfig? = null
    var activeProtocolName: String = "Socket API"
        private set

    private var resolvedType: ConnectionType = ConnectionType.SOCKET_API

    suspend fun connect(config: ConnectionConfig) = withContext(Dispatchers.IO) {
        currentConfig = config
        when (config.connectionType) {
            ConnectionType.AUTO -> {
                var socketError: Exception? = null
                try {
                    socketClient.connect(config)
                    resolvedType = ConnectionType.SOCKET_API
                    activeProtocolName = "Socket API (Porta ${config.port})"
                    return@withContext
                } catch (e: Exception) {
                    socketError = e
                }

                // Fallback a REST API
                try {
                    restClient.setConfig(config)
                    restClient.testConnection()
                    resolvedType = ConnectionType.REST_API
                    activeProtocolName = "REST API (Porta ${config.port})"
                    return@withContext
                } catch (restErr: Exception) {
                    val socketMsg = socketError?.localizedMessage ?: "Errore Socket"
                    val restMsg = restErr.localizedMessage ?: "Errore REST"
                    throw Exception("Impossibile connettersi a ${config.host}:${config.port}.\n\n• Provato Socket API: $socketMsg\n\n• Provato REST API: $restMsg")
                }
            }
            ConnectionType.SOCKET_API -> {
                resolvedType = ConnectionType.SOCKET_API
                activeProtocolName = "Socket API"
                socketClient.connect(config)
            }
            ConnectionType.REST_API -> {
                resolvedType = ConnectionType.REST_API
                activeProtocolName = "REST API"
                restClient.setConfig(config)
                restClient.testConnection()
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        currentConfig = null
        socketClient.close()
    }

    suspend fun fetchUsers(): List<HotspotUser> = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.fetchUsers()
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.fetchUsers()
        }
    }

    suspend fun fetchProfiles(): List<HotspotProfile> = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.fetchProfiles()
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.fetchProfiles()
        }
    }

    suspend fun addUser(user: HotspotUser) = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.addUser(user)
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.addUser(user)
        }
    }

    suspend fun updateUser(user: HotspotUser) = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.updateUser(user)
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.updateUser(user)
        }
    }

    suspend fun deleteUser(id: String) = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.deleteUser(id)
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.deleteUser(id)
        }
    }

    suspend fun setUserDisabled(id: String, disabled: Boolean) = withContext(Dispatchers.IO) {
        when (resolvedType) {
            ConnectionType.SOCKET_API -> socketClient.setUserDisabled(id, disabled)
            ConnectionType.REST_API, ConnectionType.AUTO -> restClient.setUserDisabled(id, disabled)
        }
    }
}
