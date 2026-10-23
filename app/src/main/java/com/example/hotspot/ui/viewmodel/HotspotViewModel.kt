package com.example.hotspot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotspot.data.local.ConnectionPreferences
import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.ConnectionState
import com.example.hotspot.data.model.HotspotProfile
import com.example.hotspot.data.model.HotspotUser
import com.example.hotspot.data.repository.HotspotRepository
import com.example.hotspot.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HotspotUiState(
    val config: ConnectionConfig = ConnectionConfig(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val users: List<HotspotUser> = emptyList(),
    val profiles: List<HotspotProfile> = emptyList(),
    val searchQuery: String = "",
    val selectedProfileFilter: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userMessage: String? = null,
    val userToEdit: HotspotUser? = null,
    val showUserDialog: Boolean = false,
    val userToDelete: HotspotUser? = null,
    val showConfigDialog: Boolean = false
)

class HotspotViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ConnectionPreferences(application)
    private val repository = HotspotRepository()

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    init {
        val savedConfig = prefs.getConfig()
        _uiState.update { it.copy(config = savedConfig) }
        // Avvia connessione automatica all'avvio se c'è un host salvato
        if (savedConfig.host.isNotBlank()) {
            connect(savedConfig)
        }
    }

    fun updateConfig(config: ConnectionConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun connect(config: ConnectionConfig = _uiState.value.config) {
        viewModelScope.launch {
            prefs.saveConfig(config)
            _uiState.update {
                it.copy(
                    config = config,
                    connectionState = ConnectionState.Connecting,
                    isLoading = true,
                    showConfigDialog = false
                )
            }

            try {
                // Prova 1: Routing predefinito Android (velocissimo su One UI 9 / LAN standard / VPN / Dati)
                NetworkUtils.unbindNetwork(getApplication())
                repository.connect(config)
            } catch (_: Exception) {
                // Prova 2: Se fallisce, forza l'interfaccia Wi-Fi specifica per reti HotSpot locali senza Internet
                try {
                    NetworkUtils.forceWifiNetwork(getApplication())
                    repository.connect(config)
                } catch (e2: Exception) {
                    NetworkUtils.unbindNetwork(getApplication())
                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Error(e2.localizedMessage ?: "Errore di connessione"),
                            isLoading = false,
                            userMessage = e2.localizedMessage
                        )
                    }
                    return@launch
                }
            }

            val workingConfig = repository.effectiveConfig
            prefs.saveConfig(workingConfig)
            _uiState.update {
                it.copy(
                    config = workingConfig,
                    connectionState = ConnectionState.Connected(
                        host = workingConfig.host,
                        port = workingConfig.port,
                        activeProtocol = repository.activeProtocolName
                    ),
                    userMessage = "Connesso a ${workingConfig.host}:${workingConfig.port} (${repository.activeProtocolName})"
                )
            }
            loadData()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                repository.disconnect()
            } catch (_: Exception) {
            }
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Disconnected,
                    users = emptyList(),
                    profiles = emptyList(),
                    userMessage = "Disconnesso"
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val users = repository.fetchUsers()
                val profiles = repository.fetchProfiles()
                _uiState.update {
                    it.copy(
                        users = users,
                        profiles = profiles,
                        isRefreshing = false,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isLoading = false,
                        userMessage = "Errore aggiornamento: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun loadData() {
        try {
            val users = repository.fetchUsers()
            val profiles = repository.fetchProfiles()
            _uiState.update {
                it.copy(
                    users = users,
                    profiles = profiles,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = "Errore caricamento dati: ${e.localizedMessage}"
                )
            }
        }
    }

    fun openAddUserDialog() {
        _uiState.update {
            it.copy(
                userToEdit = HotspotUser(),
                showUserDialog = true
            )
        }
    }

    fun openEditUserDialog(user: HotspotUser) {
        _uiState.update {
            it.copy(
                userToEdit = user,
                showUserDialog = true
            )
        }
    }

    fun closeUserDialog() {
        _uiState.update {
            it.copy(
                showUserDialog = false,
                userToEdit = null
            )
        }
    }

    fun openDeleteConfirmation(user: HotspotUser) {
        _uiState.update { it.copy(userToDelete = user) }
    }

    fun closeDeleteConfirmation() {
        _uiState.update { it.copy(userToDelete = null) }
    }

    fun saveUser(user: HotspotUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showUserDialog = false) }
            try {
                if (user.id.isBlank()) {
                    repository.addUser(user)
                    _uiState.update { it.copy(userMessage = "Utente '${user.name}' creato con successo") }
                } else {
                    repository.updateUser(user)
                    _uiState.update { it.copy(userMessage = "Utente '${user.name}' modificato con successo") }
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Errore salvataggio utente: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deleteUser(user: HotspotUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userToDelete = null) }
            try {
                repository.deleteUser(user.id)
                _uiState.update { it.copy(userMessage = "Utente '${user.name}' eliminato") }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Errore eliminazione utente: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun toggleUserDisabled(user: HotspotUser) {
        viewModelScope.launch {
            val newDisabledState = !user.disabled
            try {
                repository.setUserDisabled(user.id, newDisabledState)
                _uiState.update {
                    val statusText = if (newDisabledState) "disabilitato" else "abilitato"
                    it.copy(userMessage = "Utente '${user.name}' $statusText")
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "Errore cambio stato: ${e.localizedMessage}")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateProfileFilter(profileName: String?) {
        _uiState.update { it.copy(selectedProfileFilter = profileName) }
    }

    fun openConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true) }
    }

    fun closeConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
