package com.example.hotspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hotspot.data.model.ConnectionState
import com.example.hotspot.ui.components.ConnectionConfigDialog
import com.example.hotspot.ui.components.DeleteConfirmDialog
import com.example.hotspot.ui.components.UserCard
import com.example.hotspot.ui.components.UserFormDialog
import com.example.hotspot.ui.viewmodel.HotspotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHotspotScreen(
    viewModel: HotspotViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MikroTik HotSpot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (dotColor, statusText) = when (val state = uiState.connectionState) {
                                is ConnectionState.Connected -> Color(0xFF4CAF50) to "Connesso (${state.host}:${uiState.config.port})"
                                is ConnectionState.Connecting -> Color(0xFFFFC107) to "Connessione in corso..."
                                is ConnectionState.Error -> Color(0xFFF44336) to "Errore connessione"
                                is ConnectionState.Disconnected -> Color(0xFF9E9E9E) to "Disconnesso"
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.connectionState is ConnectionState.Connected) {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.PowerOff, contentDescription = "Disconnetti", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { viewModel.connect() }) {
                            Icon(Icons.Default.Power, contentDescription = "Connetti", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.openConfigDialog() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni Connessione")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            if (uiState.connectionState is ConnectionState.Connected) {
                FloatingActionButton(
                    onClick = { viewModel.openAddUserDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi Utente")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading || uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (val state = uiState.connectionState) {
                is ConnectionState.Disconnected, is ConnectionState.Error -> {
                    DisconnectedContent(
                        configHost = uiState.config.host,
                        configPort = uiState.config.port,
                        errorMessage = (state as? ConnectionState.Error)?.message,
                        onConnectClick = { viewModel.connect() },
                        onOpenSettingsClick = { viewModel.openConfigDialog() }
                    )
                }

                is ConnectionState.Connecting -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Connessione a ${uiState.config.host}:${uiState.config.port}...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                is ConnectionState.Connected -> {
                    val filteredUsers = uiState.users.filter { user ->
                        val matchesSearch = uiState.searchQuery.isBlank() ||
                                user.name.contains(uiState.searchQuery, ignoreCase = true) ||
                                user.comment.contains(uiState.searchQuery, ignoreCase = true)
                        val matchesProfile = uiState.selectedProfileFilter == null ||
                                user.profile.equals(uiState.selectedProfileFilter, ignoreCase = true)
                        matchesSearch && matchesProfile
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Summary Cards
                        StatsSummaryRow(
                            totalUsers = uiState.users.size,
                            activeUsers = uiState.users.count { !it.disabled },
                            disabledUsers = uiState.users.count { it.disabled }
                        )

                        // Search and Filter Bar
                        SearchAndFilterBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            profiles = uiState.profiles.map { it.name }.distinct(),
                            selectedProfile = uiState.selectedProfileFilter,
                            onProfileSelected = { viewModel.updateProfileFilter(it) }
                        )

                        // User List
                        if (filteredUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.users.isEmpty()) "Nessun utente hotspot presente nel router." else "Nessun utente corrisponde ai filtri.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredUsers, key = { it.id.ifBlank { it.name } }) { user ->
                                    UserCard(
                                        user = user,
                                        onToggleDisabled = { viewModel.toggleUserDisabled(user) },
                                        onEdit = { viewModel.openEditUserDialog(user) },
                                        onDelete = { viewModel.openDeleteConfirmation(user) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showConfigDialog) {
        ConnectionConfigDialog(
            initialConfig = uiState.config,
            onDismiss = { viewModel.closeConfigDialog() },
            onConnect = { newConfig ->
                viewModel.connect(newConfig)
            }
        )
    }

    if (uiState.showUserDialog) {
        UserFormDialog(
            userToEdit = uiState.userToEdit,
            profiles = uiState.profiles,
            onDismiss = { viewModel.closeUserDialog() },
            onSave = { savedUser ->
                viewModel.saveUser(savedUser)
            }
        )
    }

    uiState.userToDelete?.let { userToDelete ->
        DeleteConfirmDialog(
            user = userToDelete,
            onDismiss = { viewModel.closeDeleteConfirmation() },
            onConfirm = { viewModel.deleteUser(userToDelete) }
        )
    }
}

@Composable
fun DisconnectedContent(
    configHost: String,
    configPort: Int,
    errorMessage: String?,
    onConnectClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gestione MikroTik HotSpot",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connettiti alla tua antenna o router MikroTik sulla porta $configPort per leggere, creare e modificare gli utenti HotSpot.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Power, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connetti a $configHost:$configPort")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Impostazioni IP / Porta")
                }
            }
        }
    }
}

@Composable
fun StatsSummaryRow(
    totalUsers: Int,
    activeUsers: Int,
    disabledUsers: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Totali",
            count = totalUsers.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Attivi",
            count = activeUsers.toString(),
            color = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Disabilitati",
            count = disabledUsers.toString(),
            color = Color(0xFFC62828),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    profiles: List<String>,
    selectedProfile: String?,
    onProfileSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Cerca utente o nota...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancella ricerca")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (profiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )

                FilterChip(
                    selected = selectedProfile == null,
                    onClick = { onProfileSelected(null) },
                    label = { Text("Tutti i profili") }
                )

                profiles.forEach { profileName ->
                    FilterChip(
                        selected = selectedProfile == profileName,
                        onClick = {
                            if (selectedProfile == profileName) onProfileSelected(null)
                            else onProfileSelected(profileName)
                        },
                        label = { Text(profileName) }
                    )
                }
            }
        }
    }
}
