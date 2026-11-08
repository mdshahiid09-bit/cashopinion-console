package com.cashopinion.console.ui.screens.settings

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
import com.cashopinion.console.BuildConfig
import com.cashopinion.console.R
import com.cashopinion.console.core.Constants
import com.cashopinion.console.core.SessionManager
import com.cashopinion.console.data.repo.AuthRepo
import com.cashopinion.console.data.repo.ServerClient
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    serverClient: ServerClient,
    authRepo: AuthRepo,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val isDark by sessionManager.isDarkMode.collectAsState()

    var serverUrlInput by remember { mutableStateOf(sessionManager.serverUrl) }
    var healthStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingHealth by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // Appearance
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Gold Theme", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isDark) "Enabled (Default)" else "Light theme active",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { sessionManager.isDarkTheme = it }
                    )
                }
            }
        }

        // Security & Inactivity Session
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Security & Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Inactivity Auto-Lock", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Locks app after 15 minutes of inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Surface(
                        shape = Shapes.Chip,
                        color = colors.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "15 MIN",
                            color = colors.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Render Server Config
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Money Logic Backend (Render)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Render Node.js service endpoint for atomic money transitions",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = {
                        serverUrlInput = it
                        sessionManager.serverUrl = it
                    },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (healthStatus != null) {
                    Text(text = healthStatus!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        isCheckingHealth = true
                        healthStatus = null
                        scope.launch {
                            val res = serverClient.checkHealth()
                            isCheckingHealth = false
                            healthStatus = if (res.getOrDefault(false)) "Server is online & reachable!" else "Could not reach server: ${res.exceptionOrNull()?.message}"
                        }
                    },
                    enabled = !isCheckingHealth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCheckingHealth) "Testing..." else "Test Server Health")
                }
            }
        }

        // About
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text("Version: 2.0.0 (Build ${BuildConfig.VERSION_CODE})", color = colors.textSecondary)
                Text("Package: com.cashopinion.console", color = colors.textSecondary)
                Text("Project ID: cash-opinion", color = colors.textSecondary)
                Text("Owner: officialshahid156@gmail.com", color = colors.textSecondary)
            }
        }

        // Sign Out
        OutlinedButton(
            onClick = {
                authRepo.signOut()
                sessionManager.clearSession()
                onSignOut()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}
