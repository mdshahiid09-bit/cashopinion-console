package com.cashopinion.console.ui.screens.tasks

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
import com.cashopinion.console.R
import com.cashopinion.console.data.model.TaskItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.SearchField
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allTasks by consoleRepo.observeTasks().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Draft, 1: Published, 2: Archived
    val tabTitles = listOf("Drafts", "Published", "Archived")
    var searchQuery by remember { mutableStateOf("") }

    // Editor dialog
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var isNewTask by remember { mutableStateOf(false) }

    val filteredList = remember(allTasks, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "draft"
            1 -> "published"
            2 -> "archived"
            else -> "published"
        }
        allTasks.filter { t ->
            t.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() || t.title.contains(searchQuery, ignoreCase = true) || t.description.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Task Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    editingTask = TaskItem(status = "draft")
                    isNewTask = true
                }
            ) {
                Text("Create Task")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hint = "Search tasks..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier.clip(Shapes.Chip)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
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

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_document,
                message = "No ${tabTitles[selectedTab].lowercase()} tasks found"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.taskId }) { task ->
                    TaskCardItem(
                        task = task,
                        onEdit = {
                            editingTask = task
                            isNewTask = false
                        },
                        onArchive = {
                            scope.launch {
                                consoleRepo.archiveTask(task.taskId)
                            }
                        }
                    )
                }
            }
        }
    }

    // Task Editor Dialog
    if (editingTask != null) {
        var title by remember { mutableStateOf(editingTask!!.title) }
        var desc by remember { mutableStateOf(editingTask!!.description) }
        var link by remember { mutableStateOf(editingTask!!.link) }
        var rewardStr by remember { mutableStateOf(editingTask!!.reward.toString()) }
        var status by remember { mutableStateOf(editingTask!!.status) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) editingTask = null },
            title = {
                Text(
                    text = if (isNewTask) "Create Task" else "Edit Task",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Target Link / URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rewardStr,
                        onValueChange = { rewardStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Reward Coins") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Publish Immediately", color = colors.textSecondary)
                        Switch(
                            checked = status == "published",
                            onCheckedChange = { isPub -> status = if (isPub) "published" else "draft" }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rew = rewardStr.toLongOrNull() ?: 0L
                        if (title.isNotBlank() && rew > 0) {
                            isSaving = true
                            scope.launch {
                                consoleRepo.saveTask(
                                    editingTask!!.copy(
                                        title = title,
                                        description = desc,
                                        link = link,
                                        reward = rew,
                                        status = status
                                    )
                                )
                                isSaving = false
                                editingTask = null
                            }
                        }
                    },
                    enabled = !isSaving && title.isNotBlank() && (rewardStr.toLongOrNull() ?: 0L) > 0
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val colors = CashTheme.colors
    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = Shapes.Chip,
                    color = colors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+${task.reward} Coins",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Edit")
                }

                if (task.status != "archived") {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Archive")
                    }
                }
            }
        }
    }
}
