package com.cashopinion.console.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.ProblemReportItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allReports by consoleRepo.observeProblemReports().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Open", "Resolved")
    var searchQuery by remember { mutableStateOf("") }

    var selectedReport by remember { mutableStateOf<ProblemReportItem?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    val filteredList = remember(allReports, selectedTab, searchQuery) {
        val targetStatus = if (selectedTab == 0) "open" else "resolved"
        allReports.filter {
            it.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            it.uid.contains(searchQuery, ignoreCase = true) ||
                            it.message.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true))
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
            hint = "Search reports by category, UID, or text...",
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
                iconRes = R.drawable.ic_warning,
                message = "No ${tabTitles[selectedTab].lowercase()} problem reports."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { report ->
                    LuxuryCard(onClick = { selectedReport = report }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category: ${report.category.capitalize()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CashColors.GoldPrimary
                            )
                            StatusBadge(status = report.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "User UID: ${report.uid} · Device: ${report.deviceInfo.ifBlank { "Standard Android" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

                        if (report.adminReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin Note: ${report.adminReply}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(report.createdAt))
                        Text(
                            text = "Reported on $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (selectedReport != null) {
        val rep = selectedReport!!
        AlertDialog(
            onDismissRequest = { if (!isReplying) selectedReport = null },
            title = { Text("Manage Problem Report", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category: ${rep.category.capitalize()}", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("Details: ${rep.message}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Resolution note / reply") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isReplying = true
                            scope.launch {
                                consoleRepo.replyProblemReport(rep.id, replyText.trim(), "resolved")
                                SoundFx.playSuccess()
                                isReplying = false
                                selectedReport = null
                                replyText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                    ) {
                        Text("Mark Resolved", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReport = null }) {
                    Text("Dismiss", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}
