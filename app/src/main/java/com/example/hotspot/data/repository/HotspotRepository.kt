package com.example.hotspot.data.repository

import com.example.hotspot.data.model.AuthenticationException
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
    val effectiveConfig: ConnectionConfig
        get() = currentConfig ?: ConnectionConfig()

    var activeProtocolName: String = "Socket API"
        private set

    private var resolvedType: ConnectionType = ConnectionType.SOCKET_API

    suspend fun connect(config: ConnectionConfig) = withContext(Dispatchers.IO) {
        currentConfig = config
        when (config.connectionType) {
            ConnectionType.AUTO -> {
                val errors = mutableListOf<String>()

                // 1. Prova Socket API sulla porta configurata dall'utente (es. 8278)
                try {
                    socketClient.connect(config, timeoutMs = 2000)
                    resolvedType = ConnectionType.SOCKET_API
                    activeProtocolName = "Socket API (Porta ${config.port})"
                    return@withContext
                } catch (e: AuthenticationException) {
                    resolvedType = ConnectionType.SOCKET_API
                    activeProtocolName = "Socket API (Porta ${config.port})"
                    throw e
                } catch (e: Exception) {
                    errors.add("Socket API (${config.port}): ${e.localizedMessage}")
                }

                // 2. Prova REST API sulla porta standard HTTP 80 (veloce se il servizio www è attivo su RouterOS v7)
                try {
                    val stdRestConfig = config.copy(port = 80, useSsl = false)
                    restClient.setConfig(stdRestConfig)
                    restClient.testConnection(timeoutMs = 3000)
                    currentConfig = stdRestConfig
                    resolvedType = ConnectionType.REST_API
                    activeProtocolName = "REST API (Porta 80)"
                    return@withContext
                } catch (e: AuthenticationException) {
                    currentConfig = config.copy(port = 80, useSsl = false)
                    resolvedType = ConnectionType.REST_API
                    activeProtocolName = "REST API (Porta 80)"
                    throw e
                } catch (e: Exception) {
                    errors.add("REST API (80): ${e.localizedMessage}")
                }

                // 3. Prova Socket API SSL sulla porta standard 8729
                try {
                    val sslSocketConfig = config.copy(port = 8729, useSsl = true)
                    socketClient.connect(sslSocketConfig, timeoutMs = 2000)
                    currentConfig = sslSocketConfig
                    resolvedType = ConnectionType.SOCKET_API
                    activeProtocolName = "Socket API SSL (Porta 8729)"
                    return@withContext
                } catch (e: AuthenticationException) {
                    currentConfig = config.copy(port = 8729, useSsl = true)
                    resolvedType = ConnectionType.SOCKET_API
                    activeProtocolName = "Socket API SSL (Porta 8729)"
                    throw e
                } catch (e: Exception) {
                    errors.add("Socket API SSL (8729): ${e.localizedMessage}")
                }

                // 4. Prova Socket API sulla porta standard 8728
                if (config.port != 8728) {
                    try {
                        val stdSocketConfig = config.copy(port = 8728, useSsl = false)
                        socketClient.connect(stdSocketConfig, timeoutMs = 2000)
                        currentConfig = stdSocketConfig
                        resolvedType = ConnectionType.SOCKET_API
                        activeProtocolName = "Socket API (Porta 8728)"
                        return@withContext
                    } catch (e: AuthenticationException) {
                        currentConfig = config.copy(port = 8728, useSsl = false)
                        resolvedType = ConnectionType.SOCKET_API
                        activeProtocolName = "Socket API (Porta 8728)"
                        throw e
                    } catch (e: Exception) {
                        errors.add("Socket API (8728): ${e.localizedMessage}")
                    }
                }

                // 5. Prova REST API sulla porta configurata dall'utente (se diversa da 80)
                if (config.port != 80) {
                    try {
                        restClient.setConfig(config)
                        restClient.testConnection(timeoutMs = 2000)
                        resolvedType = ConnectionType.REST_API
                        activeProtocolName = "REST API (Porta ${config.port})"
                        return@withContext
                    } catch (e: AuthenticationException) {
                        resolvedType = ConnectionType.REST_API
                        activeProtocolName = "REST API (Porta ${config.port})"
                        throw e
                    } catch (e: Exception) {
                        errors.add("REST API (${config.port}): ${e.localizedMessage}")
                    }
                }

                // 6. Prova REST API HTTPS sulla porta standard 443
                try {
                    val httpsRestConfig = config.copy(port = 443, useSsl = true)
                    restClient.setConfig(httpsRestConfig)
                    restClient.testConnection(timeoutMs = 2000)
                    currentConfig = httpsRestConfig
                    resolvedType = ConnectionType.REST_API
                    activeProtocolName = "REST API HTTPS (Porta 443)"
                    return@withContext
                } catch (e: AuthenticationException) {
                    currentConfig = config.copy(port = 443, useSsl = true)
                    resolvedType = ConnectionType.REST_API
                    activeProtocolName = "REST API HTTPS (Porta 443)"
                    throw e
                } catch (e: Exception) {
                    errors.add("REST API HTTPS (443): ${e.localizedMessage}")
                }

                // Messaggio di errore completo se tutte le combinazioni falliscono
                val detailMsg = errors.joinToString("\n• ")
                throw Exception("Impossibile connettersi a ${config.host}.\n\nTentativi automatici riscontrati:\n• $detailMsg")
            }
            ConnectionType.SOCKET_API -> {
                resolvedType = ConnectionType.SOCKET_API
                activeProtocolName = if (config.useSsl) "Socket API SSL" else "Socket API"
                socketClient.connect(config)
            }
            ConnectionType.REST_API -> {
                resolvedType = ConnectionType.REST_API
                activeProtocolName = if (config.useSsl) "REST API HTTPS" else "REST API"
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
