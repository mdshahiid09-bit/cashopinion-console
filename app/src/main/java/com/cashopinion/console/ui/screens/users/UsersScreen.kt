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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.UserItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.SearchField
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UsersScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
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
                        u.phone.contains(searchQuery, ignoreCase = true) ||
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
            hint = "Search by name, email, UID, code, phone, device, IP..."
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
                        onClick = { selectedUser = user },
                        onCopyUid = {
                            clipboardManager.setText(AnnotatedString(user.uid))
                        }
                    )
                }
            }
        }
    }

    // User Detail & Action Modal Dialog
    if (selectedUser != null) {
        val user = selectedUser!!
        var detailTab by remember { mutableIntStateOf(0) }
        val detailTabs = listOf("Profile", "Wallet & Actions")

        val joinDateStr = remember(user.createdAt) {
            if (user.createdAt > 0) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(user.createdAt))
            } else "Not provided"
        }

        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (user.name.isNotBlank()) user.name else "Not provided",
                            color = if (user.name.isNotBlank()) colors.textPrimary else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
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

                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = detailTab,
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.primary,
                        modifier = Modifier.clip(Shapes.Chip)
                    ) {
                        detailTabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = detailTab == idx,
                                onClick = { detailTab = idx },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (detailTab == idx) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (detailTab == idx) colors.primary else colors.textSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (detailTab == 0) {
                        // Profile Tab
                        ProfileFieldRow(label = "Full Name", value = user.name)
                        ProfileFieldRow(label = "Email Address", value = user.email)
                        ProfileFieldRow(label = "Phone Number", value = user.phone)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("User ID (UID)", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                Text(user.uid, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            }
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(user.uid)) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = "Copy UID",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Referral Code", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                Text(
                                    text = if (user.referralCode.isNotBlank()) user.referralCode else "Not provided",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (user.referralCode.isNotBlank()) colors.textPrimary else colors.textSecondary
                                )
                            }
                            if (user.referralCode.isNotBlank()) {
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(user.referralCode)) }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = "Copy Referral Code",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        ProfileFieldRow(label = "Referred By (UID)", value = user.referredBy)
                        ProfileFieldRow(label = "Device Hash", value = user.deviceHash)
                        ProfileFieldRow(label = "Joined Date", value = joinDateStr)
                        ProfileFieldRow(label = "Country", value = user.country)
                        ProfileFieldRow(
                            label = "Last Login",
                            value = if (user.lastLogin > 0) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(user.lastLogin)) else ""
                        )

                        if (user.isBanned && !user.banReason.isNullOrBlank()) {
                            ProfileFieldRow(label = "Ban Reason", value = user.banReason)
                        }
                    } else {
                        // Wallet & Actions Tab
                        Surface(
                            shape = Shapes.Card,
                            color = colors.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Wallet Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${user.coins} coins (₹${String.format("%.2f", user.coins / 1000.0)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total Withdrawn: ${user.totalWithdrawn} coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "Pending Withdrawal ID: ${user.pendingWithdrawalId ?: "None"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(4.dp))

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

    // Coin Adjustment Dialog
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
                    Text("User: ${user.name.ifBlank { user.uid }} (Current: ${user.coins} coins)", color = colors.textSecondary)
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

    // In-App Warning Dialog
    if (showWarningDialog && selectedUser != null) {
        val user = selectedUser!!
        AlertDialog(
            onDismissRequest = { if (!isSendingWarning) showWarningDialog = false },
            title = { Text("Send In-App Warning", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Send a direct message to ${user.name.ifBlank { user.uid }}. No coin penalty applied.", color = colors.textSecondary)
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
fun ProfileFieldRow(
    label: String,
    value: String
) {
    val colors = CashTheme.colors
    val displayVal = if (value.isNotBlank()) value else "Not provided"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Text(
            text = displayVal,
            style = MaterialTheme.typography.bodySmall,
            color = if (value.isNotBlank()) colors.textPrimary else colors.textSecondary
        )
    }
}

@Composable
fun UserCard(
    user: UserItem,
    onClick: () -> Unit,
    onCopyUid: () -> Unit
) {
    val colors = CashTheme.colors
    val hasName = user.name.isNotBlank()
    val hasEmail = user.email.isNotBlank()
    val displayName = if (hasName) user.name else "Not provided"
    val displayEmail = if (hasEmail) user.email else "Not provided"

    val shortUid = remember(user.uid) {
        if (user.uid.length > 10) user.uid.take(6) + "..." + user.uid.takeLast(4) else user.uid
    }

    val joinDateStr = remember(user.createdAt) {
        if (user.createdAt > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.createdAt))
        } else "Not provided"
    }

    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasName) colors.textPrimary else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = Shapes.Chip,
                            color = if (user.isBanned) colors.error.copy(alpha = 0.15f) else colors.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = user.status.uppercase(),
                                color = if (user.isBanned) colors.error else colors.success,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasEmail) colors.textSecondary else colors.textSecondary.copy(alpha = 0.7f)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCopyUid() }
                ) {
                    Text(
                        text = "UID: $shortUid",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Copy UID",
                        tint = colors.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = "Joined: $joinDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
