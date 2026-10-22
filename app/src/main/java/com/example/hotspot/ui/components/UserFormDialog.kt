package com.example.hotspot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.hotspot.data.model.HotspotProfile
import com.example.hotspot.data.model.HotspotUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormDialog(
    userToEdit: HotspotUser?,
    profiles: List<HotspotProfile>,
    onDismiss: () -> Unit,
    onSave: (HotspotUser) -> Unit
) {
    val isNewUser = userToEdit == null || userToEdit.id.isBlank()

    var name by remember { mutableStateOf(userToEdit?.name ?: "") }
    var password by remember { mutableStateOf(userToEdit?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(userToEdit?.profile?.ifBlank { "default" } ?: "default") }
    var comment by remember { mutableStateOf(userToEdit?.comment ?: "") }
    var server by remember { mutableStateOf(userToEdit?.server ?: "all") }
    var limitUptime by remember { mutableStateOf(userToEdit?.limitUptime ?: "") }
    var disabled by remember { mutableStateOf(userToEdit?.disabled ?: false) }

    var expandedProfileDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNewUser) "Nuovo Utente Hotspot" else "Modifica Utente '${userToEdit?.name}'",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Utente / Username *") },
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

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedProfileDropdown,
                    onExpandedChange = { expandedProfileDropdown = !expandedProfileDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = profile,
                        onValueChange = { profile = it },
                        label = { Text("Profilo Hotspot") },
                        leadingIcon = { Icon(Icons.Default.GroupWork, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProfileDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expandedProfileDropdown,
                        onDismissRequest = { expandedProfileDropdown = false }
                    ) {
                        profiles.forEach { profileItem ->
                            DropdownMenuItem(
                                text = { Text(profileItem.name) },
                                onClick = {
                                    profile = profileItem.name
                                    expandedProfileDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server Hotspot (es. all)") },
                    leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("Limite Tempo (es. 1h, 1d, 01:00:00)") },
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Note / Commento") },
                    leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Disabilita utente",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = disabled,
                        onCheckedChange = { disabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val userToSave = HotspotUser(
                        id = userToEdit?.id ?: "",
                        name = name.trim(),
                        password = password,
                        profile = profile.trim().ifBlank { "default" },
                        comment = comment.trim(),
                        disabled = disabled,
                        server = server.trim().ifBlank { "all" },
                        limitUptime = limitUptime.trim()
                    )
                    onSave(userToSave)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isNewUser) "Crea Utente" else "Salva Modifiche")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
