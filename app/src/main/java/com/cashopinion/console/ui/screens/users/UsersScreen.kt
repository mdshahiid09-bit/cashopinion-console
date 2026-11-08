package com.cashopinion.console.ui.screens.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.UserItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.SearchField
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allUsers by consoleRepo.observeUsers().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserItem?>(null) }

    // Dialog states
    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustDirection by remember { mutableStateOf("add") }
    var adjustAmount by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf("") }
    var isAdjusting by remember { mutableStateOf(false) }

    var showWarningDialog by remember { mutableStateOf(false) }
    var warningMessage by remember { mutableStateOf("") }
    var isSendingWarning by remember { mutableStateOf(false) }

    val filteredList = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) allUsers
        else {
            allUsers.filter { u ->
                u.name.contains(searchQuery, ignoreCase = true) ||
                        u.email.contains(searchQuery, ignoreCase = true) ||
                        u.uid.contains(searchQuery, ignoreCase = true) ||
                        u.referralCode.contains(searchQuery, ignoreCase = true) ||
                        u.deviceHash.contains(searchQuery, ignoreCase = true) ||
                        u.ipAddress.contains(searchQuery, ignoreCase = true) ||
                        u.status.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Text(
            text = "User Directory",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search by name, email, UID, code, device, IP..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_user,
                message = "No users found"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.uid }) { user ->
                    UserCard(
                        user = user,
                        onClick = { selectedUser = user }
                    )
                }
            }
        }
    }

    // User Detail & Action Modal Sheet / Dialog
    if (selectedUser != null) {
        val user = selectedUser!!
        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = user.name, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = Shapes.Chip,
                        color = if (user.isBanned) colors.error.copy(alpha = 0.15f) else colors.success.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = user.status.uppercase(),
                            color = if (user.isBanned) colors.error else colors.success,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Email: ${user.email.ifBlank { "N/A" }}", color = colors.textSecondary)
                    Text("UID: ${user.uid}", color = colors.textSecondary)
                    Text("Balance: ${user.coins} coins", color = colors.primary, fontWeight = FontWeight.SemiBold)
                    Text("Total Withdrawn: ${user.totalWithdrawn} coins", color = colors.textSecondary)
                    if (user.referralCode.isNotBlank()) Text("Referral Code: ${user.referralCode}", color = colors.textSecondary)
                    if (user.deviceHash.isNotBlank()) Text("Device: ${user.deviceHash}", color = colors.textSecondary)
                    if (user.ipAddress.isNotBlank()) Text("IP: ${user.ipAddress}", color = colors.textSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                adjustDirection = "add"
                                adjustAmount = ""
                                adjustReason = ""
                                showAdjustDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Coins")
                        }
                        Button(
                            onClick = {
                                adjustDirection = "deduct"
                                adjustAmount = ""
                                adjustReason = ""
                                showAdjustDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deduct")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                warningMessage = ""
                                showWarningDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Send Warning")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    if (user.isBanned) {
                                        consoleRepo.serverClient.unbanUser(user.uid)
                                    } else {
                                        consoleRepo.serverClient.banUser(user.uid, "Admin action")
                                    }
                                    selectedUser = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isBanned) colors.success else colors.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (user.isBanned) "Unban" else "Ban User")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedUser = null }) {
                    Text("Close", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    // Coin Adjustment Dialog (Reason optional, deduct >= 0)
    if (showAdjustDialog && selectedUser != null) {
        val user = selectedUser!!
        AlertDialog(
            onDismissRequest = { if (!isAdjusting) showAdjustDialog = false },
            title = {
                Text(
                    text = if (adjustDirection == "add") "Add Coins to User" else "Deduct Coins from User",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("User: ${user.name} (Current: ${user.coins} coins)", color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (Coins)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustReason,
                        onValueChange = { adjustReason = it },
                        label = { Text("Reason (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmount.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            isAdjusting = true
                            scope.launch {
                                consoleRepo.serverClient.adjustCoins(user.uid, amt, adjustDirection, adjustReason)
                                isAdjusting = false
                                showAdjustDialog = false
                                selectedUser = null
                            }
                        }
                    },
                    enabled = !isAdjusting && (adjustAmount.toLongOrNull() ?: 0L) > 0
                ) {
                    Text(if (isAdjusting) "Saving..." else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    // In-App Warning Dialog (Message only, no penalty)
    if (showWarningDialog && selectedUser != null) {
        val user = selectedUser!!
        AlertDialog(
            onDismissRequest = { if (!isSendingWarning) showWarningDialog = false },
            title = { Text("Send In-App Warning", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Send a direct message to ${user.name}. No coin penalty applied.", color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = warningMessage,
                        onValueChange = { warningMessage = it },
                        label = { Text("Warning Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (warningMessage.isNotBlank()) {
                            isSendingWarning = true
                            scope.launch {
                                consoleRepo.sendUserWarning(user.uid, warningMessage)
                                isSendingWarning = false
                                showWarningDialog = false
                            }
                        }
                    },
                    enabled = !isSendingWarning && warningMessage.isNotBlank()
                ) {
                    Text(if (isSendingWarning) "Sending..." else "Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

@Composable
fun UserCard(
    user: UserItem,
    onClick: () -> Unit
) {
    val colors = CashTheme.colors
    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (user.isBanned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = Shapes.Chip,
                            color = colors.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "BANNED",
                                color = colors.error,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = user.email.ifBlank { user.uid },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${user.coins} coins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = "Withdrawn: ${user.totalWithdrawn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
