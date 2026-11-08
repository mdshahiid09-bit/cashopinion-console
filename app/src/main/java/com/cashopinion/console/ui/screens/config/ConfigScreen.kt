package com.cashopinion.console.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.core.Constants
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ConfigScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var isMaintenanceEnabled by remember { mutableStateOf(false) }
    var maintenanceTitle by remember { mutableStateOf("Under Scheduled Maintenance") }
    var maintenanceMessage by remember { mutableStateOf("We are improving our platform. Please check back shortly.") }

    var latestVersionCode by remember { mutableStateOf("138") }
    var minVersionCode by remember { mutableStateOf("138") }
    var isForceUpdate by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("https://github.com/mdshahiid09-bit/cashopinion/releases/latest") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseDatabase.getInstance()
            val snap = db.getReference(Constants.NODE_CONFIG_APP).get().await()
            if (snap.exists()) {
                val mSnap = snap.child("maintenance")
                isMaintenanceEnabled = mSnap.child("enabled").value == true
                maintenanceTitle = mSnap.child("title").value?.toString() ?: maintenanceTitle
                maintenanceMessage = mSnap.child("message").value?.toString() ?: maintenanceMessage

                val uSnap = snap.child("update")
                latestVersionCode = uSnap.child("latestVersionCode").value?.toString() ?: "138"
                minVersionCode = uSnap.child("minVersionCode").value?.toString() ?: "138"
                isForceUpdate = uSnap.child("force").value == true || uSnap.child("forceUpdate").value == true
                updateUrl = uSnap.child("url").value?.toString() ?: updateUrl
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // Maintenance Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Maintenance Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Switch(
                        checked = isMaintenanceEnabled,
                        onCheckedChange = { isMaintenanceEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = maintenanceTitle,
                    onValueChange = { maintenanceTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maintenanceMessage,
                    onValueChange = { maintenanceMessage = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // App Update Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version & Updates Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                OutlinedTextField(
                    value = latestVersionCode,
                    onValueChange = { latestVersionCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Latest Version Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minVersionCode,
                    onValueChange = { minVersionCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Minimum Supported Version Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = updateUrl,
                    onValueChange = { updateUrl = it },
                    label = { Text("Update Download URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Force Update", color = colors.textSecondary)
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it }
                    )
                }
            }
        }

        if (statusMessage != null) {
            Text(text = statusMessage!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isSaving = true
                statusMessage = null
                scope.launch {
                    try {
                        val db = FirebaseDatabase.getInstance()
                        val updates = mapOf(
                            "maintenance/enabled" to isMaintenanceEnabled,
                            "maintenance/title" to maintenanceTitle,
                            "maintenance/message" to maintenanceMessage,
                            "update/latestVersionCode" to (latestVersionCode.toLongOrNull() ?: 138L),
                            "update/minVersionCode" to (minVersionCode.toLongOrNull() ?: 138L),
                            "update/minSupportedVersionCode" to (minVersionCode.toLongOrNull() ?: 138L),
                            "update/force" to isForceUpdate,
                            "update/forceUpdate" to isForceUpdate,
                            "update/url" to updateUrl,
                            "update/updateUrl" to updateUrl
                        )
                        db.getReference(Constants.NODE_CONFIG_APP).updateChildren(updates).await()
                        statusMessage = "Configuration saved live!"
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSaving = false
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save Configuration")
        }
    }
}
