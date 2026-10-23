package com.example.hotspot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.hotspot.data.model.ConnectionConfig
import com.example.hotspot.data.model.ConnectionType

@Composable
fun ConnectionConfigDialog(
    initialConfig: ConnectionConfig,
    onDismiss: () -> Unit,
    onConnect: (ConnectionConfig) -> Unit
) {
    var host by remember { mutableStateOf(initialConfig.host) }
    var portText by remember { mutableStateOf(initialConfig.port.toString()) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var useSsl by remember { mutableStateOf(initialConfig.useSsl) }
    var connectionType by remember { mutableStateOf(initialConfig.connectionType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Connessione MikroTik RouterOS", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Indirizzo IP / Host") },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "Consiglio Samsung / Android 14: Se il Wi-Fi non ha Internet, disattiva temporaneamente i Dati Mobili (4G/5G).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { char -> char.isDigit() } },
                    label = { Text("Porta API (es. 8728, 80, 8278)") },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Porte standard:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = { portText = "8728" },
                        label = { Text("8728 (Socket)") }
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = {
                            portText = "8729"
                            useSsl = true
                        },
                        label = { Text("8729 (SSL)") }
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = { portText = "80" },
                        label = { Text("80 (REST)") }
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = { portText = "8278" },
                        label = { Text("8278") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username MikroTik") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Nascondi password" else "Mostra password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Protocollo API",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (connectionType == ConnectionType.AUTO),
                            onClick = { connectionType = ConnectionType.AUTO }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (connectionType == ConnectionType.AUTO),
                        onClick = { connectionType = ConnectionType.AUTO }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Rilevamento Automatico (Consigliato)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Prova Socket API e poi REST API sulla porta $portText", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (connectionType == ConnectionType.SOCKET_API),
                            onClick = { connectionType = ConnectionType.SOCKET_API }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (connectionType == ConnectionType.SOCKET_API),
                        onClick = { connectionType = ConnectionType.SOCKET_API }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("RouterOS Socket API", style = MaterialTheme.typography.bodyMedium)
                        Text("Porta API binaria MikroTik (8278 / 8728)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (connectionType == ConnectionType.REST_API),
                            onClick = { connectionType = ConnectionType.REST_API }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (connectionType == ConnectionType.REST_API),
                        onClick = { connectionType = ConnectionType.REST_API }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("RouterOS v7 REST API", style = MaterialTheme.typography.bodyMedium)
                        Text("Servizio WWW HTTP/HTTPS su porta $portText", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usa connessione SSL/TLS",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = useSsl,
                        onCheckedChange = { useSsl = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8278
                    onConnect(
                        ConnectionConfig(
                            host = host.trim(),
                            port = port,
                            username = username.trim(),
                            password = password,
                            useSsl = useSsl,
                            connectionType = connectionType
                        )
                    )
                },
                enabled = host.isNotBlank() && username.isNotBlank()
            ) {
                Text("Connetti e Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
