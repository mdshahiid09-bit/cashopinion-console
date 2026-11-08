package com.cashopinion.console.ui.screens.support

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.SupportTicketItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SupportTicketsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allTickets by consoleRepo.observeSupportTickets().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Open", "In Progress", "Closed")
    var searchQuery by remember { mutableStateOf("") }

    var selectedTicket by remember { mutableStateOf<SupportTicketItem?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    val filteredList = remember(allTickets, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "open"
            1 -> "in_progress"
            else -> "closed"
        }
        allTickets.filter { ticket ->
            ticket.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            ticket.subject.contains(searchQuery, ignoreCase = true) ||
                            ticket.uid.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hint = "Search by ticket subject or user UID...",
            modifier = Modifier.padding(16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        SoundFx.playClick()
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_headset,
                message = "No ${tabTitles[selectedTab].lowercase()} support tickets."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { ticket ->
                    LuxuryCard(
                        onClick = { selectedTicket = ticket },
                        isUrgent = ticket.status == "open" && (System.currentTimeMillis() - ticket.createdAt) > 48 * 3600 * 1000L
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ticket #${ticket.id.take(8)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            StatusBadge(status = ticket.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "User UID: ${ticket.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

                        val lastMsg = ticket.messages.lastOrNull()
                        if (lastMsg != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${if (lastMsg.sender == "admin") "Admin" else "User"}: ${lastMsg.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ticket.createdAt))
                        Text(
                            text = "Created: $dateStr · ${ticket.messages.size} message(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    // Detail & Reply Dialog
    if (selectedTicket != null) {
        val ticket = selectedTicket!!
        AlertDialog(
            onDismissRequest = { selectedTicket = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ticket #${ticket.id.take(8)}", color = colors.primary, fontWeight = FontWeight.Bold)
                    StatusBadge(status = ticket.status)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Subject: ${ticket.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "UID: ${ticket.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    HorizontalDivider(color = colors.border)

                    // Message thread
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ticket.messages) { msg ->
                            val isAdmin = msg.sender == "admin"
                            Surface(
                                shape = Shapes.Chip,
                                color = if (isAdmin) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = if (isAdmin) 24.dp else 0.dp, end = if (isAdmin) 0.dp else 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = if (isAdmin) "Admin" else "User",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAdmin) colors.primary else colors.textSecondary
                                    )
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Type reply as Admin...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ticket.status != "closed") {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    consoleRepo.updateTicketStatus(ticket.id, "closed")
                                    SoundFx.playSuccess()
                                    selectedTicket = null
                                }
                            }
                        ) {
                            Text("Close Ticket")
                        }
                    }
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                isSendingReply = true
                                scope.launch {
                                    consoleRepo.replySupportTicket(ticket.id, replyText.trim())
                                    SoundFx.playSuccess()
                                    replyText = ""
                                    isSendingReply = false
                                    selectedTicket = null
                                }
                            }
                        },
                        enabled = replyText.isNotBlank() && !isSendingReply,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text(if (isSendingReply) "Sending..." else "Send Reply", color = androidx.compose.ui.graphics.Color(0xFF140E00))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicket = null }) {
                    Text("Dismiss", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}
