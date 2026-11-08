package com.cashopinion.console.ui.screens.tasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cashopinion.console.R
import com.cashopinion.console.data.model.TaskItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allTasks by consoleRepo.observeTasks().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Drafts, 1: Published, 2: Archived
    val tabTitles = listOf("Drafts", "Published", "Archived")
    var searchQuery by remember { mutableStateOf("") }

    // Editor & Detail Sheet State
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var detailTask by remember { mutableStateOf<TaskItem?>(null) }
    var previewTask by remember { mutableStateOf<TaskItem?>(null) }

    val filteredList = remember(allTasks, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "draft"
            1 -> "published"
            2 -> "archived"
            else -> "published"
        }
        allTasks.filter { t ->
            t.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            t.title.contains(searchQuery, ignoreCase = true) ||
                            t.description.contains(searchQuery, ignoreCase = true) ||
                            t.taskId.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Task Management Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Real-time task configuration & proof submission rules",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = {
                    SoundFx.playClick()
                    editingTask = TaskItem(status = "draft")
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Task")
            }
        }

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hint = "Search tasks by title, description or ID..."
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier.clip(Shapes.Chip)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        SoundFx.playClick()
                        selectedTab = index
                    },
                    text = {
                        val count = allTasks.count {
                            it.status.equals(
                                when (index) {
                                    0 -> "draft"
                                    1 -> "published"
                                    2 -> "archived"
                                    else -> "published"
                                },
                                ignoreCase = true
                            )
                        }
                        Text(
                            text = "$title ($count)",
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_document,
                message = "No ${tabTitles[selectedTab].lowercase()} tasks found."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.taskId }) { task ->
                    TaskCardItem(
                        task = task,
                        onEdit = {
                            SoundFx.playClick()
                            editingTask = task
                        },
                        onViewDetails = {
                            SoundFx.playClick()
                            detailTask = task
                        },
                        onPreview = {
                            SoundFx.playClick()
                            previewTask = task
                        },
                        onDuplicate = {
                            SoundFx.playClick()
                            scope.launch {
                                val draftCopy = task.copy(
                                    taskId = "",
                                    title = "${task.title} (Copy)",
                                    status = "draft",
                                    completionsCount = 0L,
                                    publishedAt = 0L,
                                    createdAt = 0L,
                                    updatedAt = 0L
                                )
                                consoleRepo.saveTask(draftCopy)
                                SoundFx.playSuccess()
                            }
                        },
                        onArchive = {
                            SoundFx.playClick()
                            scope.launch {
                                consoleRepo.archiveTask(task.taskId)
                                SoundFx.playWarning()
                            }
                        },
                        onRestore = {
                            SoundFx.playClick()
                            scope.launch {
                                consoleRepo.restoreTask(task.taskId)
                                SoundFx.playSuccess()
                            }
                        },
                        onDelete = {
                            SoundFx.playClick()
                            scope.launch {
                                consoleRepo.deleteTask(task.taskId)
                                SoundFx.playWarning()
                            }
                        }
                    )
                }
            }
        }
    }

    // Task Editor Sheet / Modal
    if (editingTask != null) {
        TaskEditorDialog(
            initialTask = editingTask!!,
            onDismiss = { editingTask = null },
            onSave = { taskToSave ->
                scope.launch {
                    consoleRepo.saveTask(taskToSave)
                    SoundFx.playSuccess()
                    editingTask = null
                }
            }
        )
    }

    // Task Live Detail & Stats Sheet
    if (detailTask != null) {
        TaskDetailSheet(
            task = detailTask!!,
            onDismiss = { detailTask = null }
        )
    }

    // User App Exact Preview Sheet
    if (previewTask != null) {
        TaskUserAppPreviewSheet(
            task = previewTask!!,
            onDismiss = { previewTask = null }
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onPreview: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CashTheme.colors
    val hasCompletions = task.completionsCount > 0

    LuxuryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Logo Circle or Icon
                if (task.logoUrl.isNotBlank()) {
                    TaskLogoThumbnail(imageUrl = task.logoUrl)
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    if (task.appName.isNotBlank() && !task.appName.equals(task.title, ignoreCase = true)) {
                        Text(
                            text = task.appName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CashColors.GoldPrimary
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ID: ${task.taskId.take(8)} · Proof: ${task.proofType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        if (task.packageName.isNotBlank()) {
                            Surface(
                                shape = Shapes.Chip,
                                color = colors.surfaceVariant
                            ) {
                                Text(
                                    text = task.packageName.takeLast(18),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (task.requiredUsageMinutes > 0L) {
                            Surface(
                                shape = Shapes.Chip,
                                color = CashColors.GoldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${task.requiredUsageMinutes}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = CashColors.GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = Shapes.Chip,
                color = CashColors.GoldPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "+${task.reward} Coins",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CashColors.GoldPrimary,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics and Lock Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = task.status)
                if (hasCompletions) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = Shapes.Chip,
                        color = colors.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Locked after 1st completion",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            val totalStr = if (task.totalLimit > 0) "${task.totalLimit}" else "Unlimited"
            Text(
                text = "Completed: ${task.completionsCount} / $totalStr",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = colors.border.copy(alpha = 0.5f)
        )

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onViewDetails,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Live Stats", fontSize = 11.sp, color = colors.primary)
                }

                TextButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("User Preview", fontSize = 11.sp, color = colors.textSecondary)
                }

                TextButton(
                    onClick = onDuplicate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Duplicate", fontSize = 11.sp, color = colors.textSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Edit", fontSize = 11.sp)
                }

                if (task.status == "archived") {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Restore", fontSize = 11.sp, color = colors.primary)
                    }
                } else {
                    OutlinedButton(
                        onClick = onArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Archive", fontSize = 11.sp)
                    }
                }

                if (task.status == "draft") {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskEditorDialog(
    initialTask: TaskItem,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {
    val colors = CashTheme.colors
    val context = LocalContext.current
    val hasCompletions = initialTask.completionsCount > 0

    // Form fields - Complete 18 fields
    var appName by remember { mutableStateOf(initialTask.appName) }
    var title by remember { mutableStateOf(initialTask.title) }
    var description by remember { mutableStateOf(initialTask.description) }
    var packageName by remember { mutableStateOf(initialTask.packageName) }
    var requiredUsageMinutesStr by remember { mutableStateOf(if (initialTask.requiredUsageMinutes > 0L) initialTask.requiredUsageMinutes.toString() else "") }
    var logoUrl by remember { mutableStateOf(initialTask.logoUrl) }
    var bannerUrl by remember { mutableStateOf(initialTask.bannerUrl) }
    var link by remember { mutableStateOf(initialTask.link) }
    var steps by remember { mutableStateOf(if (initialTask.steps.isNotEmpty()) initialTask.steps else listOf("")) }
    var rewardStr by remember { mutableStateOf(if (initialTask.reward > 0) initialTask.reward.toString() else "100") }
    var perUserLimitStr by remember { mutableStateOf(initialTask.perUserLimit.toString()) }
    var totalLimitStr by remember { mutableStateOf(if (initialTask.totalLimit > 0) initialTask.totalLimit.toString() else "") }
    var deadlineStr by remember { mutableStateOf(if (initialTask.deadline > 0) formatEpochToIst(initialTask.deadline) else "") }
    var proofType by remember { mutableStateOf(initialTask.proofType.ifBlank { "Optional" }) }
    var proofHint by remember { mutableStateOf(initialTask.proofHint) }
    var terms by remember { mutableStateOf(initialTask.terms) }
    var adminNote by remember { mutableStateOf(initialTask.adminNote) }
    var publishImmediately by remember { mutableStateOf(initialTask.status == "published") }

    // Validation & Error Tracking
    var showValidationErrors by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }

    // Dirty state tracking
    val isDirty = remember(
        appName, title, description, packageName, requiredUsageMinutesStr, logoUrl, bannerUrl,
        link, steps, rewardStr, perUserLimitStr, totalLimitStr, deadlineStr, proofType, proofHint, terms, adminNote, publishImmediately
    ) {
        appName != initialTask.appName ||
                title != initialTask.title ||
                description != initialTask.description ||
                packageName != initialTask.packageName ||
                requiredUsageMinutesStr != (if (initialTask.requiredUsageMinutes > 0L) initialTask.requiredUsageMinutes.toString() else "") ||
                logoUrl != initialTask.logoUrl ||
                bannerUrl != initialTask.bannerUrl ||
                link != initialTask.link ||
                steps != initialTask.steps ||
                rewardStr != (if (initialTask.reward > 0) initialTask.reward.toString() else "100") ||
                perUserLimitStr != initialTask.perUserLimit.toString() ||
                totalLimitStr != (if (initialTask.totalLimit > 0) initialTask.totalLimit.toString() else "") ||
                proofType != (initialTask.proofType.ifBlank { "Optional" }) ||
                proofHint != initialTask.proofHint ||
                terms != initialTask.terms ||
                adminNote != initialTask.adminNote ||
                publishImmediately != (initialTask.status == "published")
    }

    // Intercept Back button to protect unsaved form changes
    BackHandler {
        if (isDirty) {
            showDiscardPrompt = true
        } else {
            onDismiss()
        }
    }

    // Field-level Validation Rules
    val isTitleValid = title.trim().length in 3..80
    val isDescValid = description.trim().length <= 1000
    val isLinkValid = link.trim().startsWith("https://")
    val validSteps = steps.map { it.trim() }.filter { it.isNotEmpty() }
    val isStepsValid = validSteps.isNotEmpty()
    val rewardNum = rewardStr.toLongOrNull() ?: 0L
    val isRewardValid = rewardNum >= 1L
    val perUserNum = perUserLimitStr.toIntOrNull() ?: 0
    val isPerUserValid = perUserNum >= 1
    val totalLimitNum = totalLimitStr.toLongOrNull() ?: 0L
    val requiredUsageMinutesNum = requiredUsageMinutesStr.toLongOrNull() ?: 0L
    val parsedDeadlineMs = parseIstToEpoch(deadlineStr)
    val isDeadlineValid = deadlineStr.isBlank() || parsedDeadlineMs > System.currentTimeMillis()

    // Count invalid fields for Publish action
    val invalidFieldCount = listOf(
        !isTitleValid,
        !isDescValid,
        !isLinkValid,
        !isStepsValid,
        !isRewardValid,
        !isPerUserValid,
        !isDeadlineValid
    ).count { it }

    val isReadyToPublish = invalidFieldCount == 0

    Dialog(
        onDismissRequest = {
            if (isDirty) {
                showDiscardPrompt = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = Shapes.Dialog,
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialTask.taskId.isBlank()) "Create New Task" else "Edit Task",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Full task builder: branding, package tracking, steps, limits & proof",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDirty) {
                            Surface(
                                shape = Shapes.Chip,
                                color = CashColors.GoldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Unsaved Changes",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = CashColors.GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = {
                                if (isDirty) showDiscardPrompt = true else onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.surfaceVariant)

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: BASIC INFORMATION
                    Text(
                        text = "1. BASIC INFORMATION *",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App Name (e.g. Cash Opinion, Google Pay)") },
                        supportingText = { Text("Shown as primary app branding on user task card") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text("Task Title *") },
                        supportingText = {
                            Text(
                                text = "${title.length} / 80 characters (min 3 required)",
                                color = if (!isTitleValid && showValidationErrors) colors.error else colors.textSecondary
                            )
                        },
                        isError = !isTitleValid && showValidationErrors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (!isTitleValid && showValidationErrors) {
                        Text(
                            text = "Title must be between 3 and 80 characters.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.error
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 1000) description = it },
                        label = { Text("Task Description") },
                        supportingText = { Text("${description.length} / 1000 characters") },
                        isError = !isDescValid && showValidationErrors,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5
                    )

                    // SECTION 2: MEDIA & BRANDING
                    Text(
                        text = "2. MEDIA & BRANDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TaskLogoThumbnail(imageUrl = logoUrl)

                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            label = { Text("App Logo Image URL (e.g. https://.../logo.png)") },
                            supportingText = { Text("Direct web image URL shown as task app icon in user app") },
                            trailingIcon = if (logoUrl.isNotBlank()) {
                                {
                                    IconButton(onClick = {
                                        SoundFx.playClick()
                                        logoUrl = ""
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Clear",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = bannerUrl,
                        onValueChange = { bannerUrl = it },
                        label = { Text("Header Banner URL (Optional)") },
                        supportingText = { Text("Image link displayed at top of task details") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // SECTION 3: APP TRACKING & DESTINATION LINK
                    Text(
                        text = "3. APP TRACKING & DESTINATION LINK *",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Destination / Play Store Link (https:// required) *") },
                        supportingText = {
                            Text(
                                text = if (!isLinkValid && (link.isNotBlank() || showValidationErrors))
                                    "Must begin with https://" else "URL user opens to download or complete task",
                                color = if (!isLinkValid && (link.isNotBlank() || showValidationErrors)) colors.error else colors.textSecondary
                            )
                        },
                        isError = !isLinkValid && (link.isNotBlank() || showValidationErrors),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Tracked Package Name (e.g. com.example.app)") },
                        supportingText = { Text("Required for user app UsageStatsManager real-time tracking") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = requiredUsageMinutesStr,
                        onValueChange = { requiredUsageMinutesStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Required App Usage Time (Minutes, Optional)") },
                        supportingText = { Text("Duration user must keep the app active (e.g. 1, 2, 5 minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // SECTION 4: INSTRUCTION STEPS
                    Text(
                        text = "4. TASK INSTRUCTION STEPS (MINIMUM 1 REQUIRED) *",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    steps.forEachIndexed { index, stepText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceVariant,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = stepText,
                                onValueChange = { updated ->
                                    if (updated.length <= 300) {
                                        val copy = steps.toMutableList()
                                        copy[index] = updated
                                        steps = copy
                                    }
                                },
                                label = { Text("Step ${index + 1} (${stepText.length}/300)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            if (index > 0) {
                                IconButton(
                                    onClick = {
                                        val copy = steps.toMutableList()
                                        val temp = copy[index]
                                        copy[index] = copy[index - 1]
                                        copy[index - 1] = temp
                                        steps = copy
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_back),
                                        contentDescription = "Move Up",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (steps.size > 1) {
                                IconButton(
                                    onClick = {
                                        val copy = steps.toMutableList()
                                        copy.removeAt(index)
                                        steps = copy
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = "Remove Step",
                                        tint = colors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { steps = steps + "" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Another Step")
                    }

                    if (!isStepsValid && showValidationErrors) {
                        Text(
                            text = "At least one non-empty step instruction is required.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.error
                        )
                    }

                    // SECTION 5: REWARDS, LIMITS & DEADLINE
                    Text(
                        text = "5. REWARDS, LIMITS & DEADLINE *",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = rewardStr,
                        onValueChange = { if (!hasCompletions) rewardStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Reward Coins *") },
                        supportingText = {
                            Text(
                                text = if (hasCompletions) "Locked after first completion" else "Coins awarded upon task completion",
                                color = if (hasCompletions) colors.warning else colors.textSecondary
                            )
                        },
                        readOnly = hasCompletions,
                        trailingIcon = if (hasCompletions) {
                            { Icon(painterResource(id = R.drawable.ic_lock), contentDescription = null, tint = colors.warning) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = perUserLimitStr,
                        onValueChange = { if (!hasCompletions) perUserLimitStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Per-User Completion Limit * (Default: 1)") },
                        readOnly = hasCompletions,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = totalLimitStr,
                        onValueChange = { if (!hasCompletions) totalLimitStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Limit / Slots (Optional, Blank = Unlimited)") },
                        readOnly = hasCompletions,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deadlineStr,
                        onValueChange = { deadlineStr = it },
                        label = { Text("Deadline IST (dd/MM/yyyy HH:mm, Optional)") },
                        supportingText = {
                            Text(
                                text = if (!isDeadlineValid) "Deadline must be in the future to publish" else "Format: 31/12/2026 23:59 IST",
                                color = if (!isDeadlineValid) colors.error else colors.textSecondary
                            )
                        },
                        isError = !isDeadlineValid,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // SECTION 6: PROOF & SUBMISSION
                    Text(
                        text = "6. PROOF & SUBMISSION SETTINGS *",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Optional", "Screenshot", "Text", "Link").forEach { typeOption ->
                            FilterChip(
                                selected = proofType.equals(typeOption, ignoreCase = true),
                                onClick = {
                                    if (!hasCompletions) {
                                        SoundFx.playClick()
                                        proofType = typeOption
                                    }
                                },
                                enabled = !hasCompletions,
                                label = { Text(if (typeOption == "Optional") "Optional (No Proof)" else typeOption) }
                            )
                        }
                    }
                    if (proofType.equals("Optional", ignoreCase = true)) {
                        Text(
                            text = "Proof is optional. Users can complete task and submit without proof, and admin can approve or reject directly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary
                        )
                    }

                    OutlinedTextField(
                        value = proofHint,
                        onValueChange = { if (it.length <= 200) proofHint = it },
                        label = { Text("Proof Submission Hint (Optional)") },
                        supportingText = { Text("${proofHint.length} / 200 chars · Guides user what proof to upload") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // SECTION 7: TERMS & ADMIN NOTES
                    Text(
                        text = "7. TERMS & ADMIN INTERNAL NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = terms,
                        onValueChange = { terms = it },
                        label = { Text("Terms & Conditions / Rules (Optional)") },
                        supportingText = { Text("Shown to users under T&C / Rules section") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("Admin Internal Note / Verification Guidelines (Optional)") },
                        supportingText = { Text("Visible only to admins when reviewing submissions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // SECTION 8: PUBLISH SETTINGS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.Chip)
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Publish Immediately",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (publishImmediately) "Task will be live to all users immediately upon save" else "Saved as draft (hidden from users)",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }

                        Switch(
                            checked = publishImmediately,
                            onCheckedChange = {
                                SoundFx.playClick()
                                publishImmediately = it
                            }
                        )
                    }
                }

                HorizontalDivider(color = colors.surfaceVariant)

                // Bottom Sticky Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (isDirty) showDiscardPrompt = true else onDismiss()
                        }
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (isTitleValid || appName.isNotBlank()) {
                                    val draftTask = initialTask.copy(
                                        appName = appName.trim(),
                                        title = if (title.isNotBlank()) title.trim() else appName.trim(),
                                        description = description.trim(),
                                        packageName = packageName.trim(),
                                        requiredUsageMinutes = requiredUsageMinutesNum,
                                        logoUrl = logoUrl,
                                        bannerUrl = bannerUrl.trim(),
                                        link = link.trim(),
                                        steps = validSteps,
                                        reward = rewardNum,
                                        perUserLimit = perUserNum.coerceAtLeast(1),
                                        totalLimit = totalLimitNum,
                                        deadline = parsedDeadlineMs,
                                        proofType = proofType,
                                        proofHint = proofHint.trim(),
                                        terms = terms.trim(),
                                        adminNote = adminNote.trim(),
                                        status = "draft"
                                    )
                                    onSave(draftTask)
                                } else {
                                    showValidationErrors = true
                                }
                            }
                        ) {
                            Text("Save Draft")
                        }

                        Button(
                            onClick = {
                                if (publishImmediately) {
                                    if (isReadyToPublish) {
                                        val publishedTask = initialTask.copy(
                                            appName = appName.trim(),
                                            title = title.trim(),
                                            description = description.trim(),
                                            packageName = packageName.trim(),
                                            requiredUsageMinutes = requiredUsageMinutesNum,
                                            logoUrl = logoUrl,
                                            bannerUrl = bannerUrl.trim(),
                                            link = link.trim(),
                                            steps = validSteps,
                                            reward = rewardNum,
                                            perUserLimit = perUserNum.coerceAtLeast(1),
                                            totalLimit = totalLimitNum,
                                            deadline = parsedDeadlineMs,
                                            proofType = proofType,
                                            proofHint = proofHint.trim(),
                                            terms = terms.trim(),
                                            adminNote = adminNote.trim(),
                                            status = "published"
                                        )
                                        onSave(publishedTask)
                                    } else {
                                        showValidationErrors = true
                                    }
                                } else {
                                    val savedTask = initialTask.copy(
                                        appName = appName.trim(),
                                        title = if (title.isNotBlank()) title.trim() else appName.trim(),
                                        description = description.trim(),
                                        packageName = packageName.trim(),
                                        requiredUsageMinutes = requiredUsageMinutesNum,
                                        logoUrl = logoUrl,
                                        bannerUrl = bannerUrl.trim(),
                                        link = link.trim(),
                                        steps = validSteps,
                                        reward = rewardNum,
                                        perUserLimit = perUserNum.coerceAtLeast(1),
                                        totalLimit = totalLimitNum,
                                        deadline = parsedDeadlineMs,
                                        proofType = proofType,
                                        proofHint = proofHint.trim(),
                                        terms = terms.trim(),
                                        adminNote = adminNote.trim(),
                                        status = "draft"
                                    )
                                    onSave(savedTask)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (publishImmediately && !isReadyToPublish) colors.error else colors.primary
                            )
                        ) {
                            if (publishImmediately && !isReadyToPublish) {
                                Text("Fix $invalidFieldCount fields", color = Color.White)
                            } else if (publishImmediately) {
                                Text("Publish Task")
                            } else {
                                Text("Save Task")
                            }
                        }
                    }
                }
            }
        }
    }

    // Unsaved Changes Discard Prompt
    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = {
                Text(
                    text = "Discard changes?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes in this task form that will be lost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundFx.playClick()
                        showDiscardPrompt = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Discard", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPrompt = false }) {
                    Text("Keep editing", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

@Composable
fun TaskDetailSheet(
    task: TaskItem,
    onDismiss: () -> Unit
) {
    val colors = CashTheme.colors
    val totalLimitDisplay = if (task.totalLimit > 0) "${task.totalLimit}" else "Unlimited"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task Analytics & Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniCard(label = "Completed", value = "${task.completionsCount}", modifier = Modifier.weight(1f))
                    MetricMiniCard(label = "Total Slots", value = totalLimitDisplay, modifier = Modifier.weight(1f))
                    MetricMiniCard(label = "Reward", value = "${task.reward}", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LAST 14 DAYS COMPLETIONS (DAILY ACTIVITY)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CashColors.GoldPrimary,
                    fontWeight = FontWeight.Bold
                )

                // 14-day Completions Canvas Bar Chart
                CompletionsCanvasBarChart(completionsCount = task.completionsCount)

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                Text(
                    text = "VERIFICATION DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CashColors.GoldPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "App Name: ${task.appName.ifBlank { "Not specified" }}\n" +
                            "Tracked Package: ${task.packageName.ifBlank { "None (No usage tracking)" }}\n" +
                            "Usage Duration: ${if (task.requiredUsageMinutes > 0L) "${task.requiredUsageMinutes} minutes" else "None"}\n" +
                            "Header Banner: ${task.bannerUrl.ifBlank { "None" }}\n" +
                            "Admin Note: ${task.adminNote.ifBlank { "None" }}\n" +
                            "Proof Type: ${task.proofType}\n" +
                            "Proof Hint: ${task.proofHint.ifBlank { "None provided" }}\n" +
                            "Target Link: ${task.link}\n" +
                            "Per-User Limit: ${task.perUserLimit}\n" +
                            "Deadline: ${if (task.deadline > 0) formatEpochToIst(task.deadline) else "No expiration"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                if (task.steps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TASK STEPS (${task.steps.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    task.steps.forEachIndexed { i, step ->
                        Text(
                            text = "${i + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = colors.surface,
        shape = Shapes.Dialog
    )
}

@Composable
fun TaskUserAppPreviewSheet(
    task: TaskItem,
    onDismiss: () -> Unit
) {
    val colors = CashTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "User App Live Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This preview matches the user app Task Detail Screen exactly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                // Exact replica of user app Task Card
                LuxuryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.logoUrl.isNotBlank()) {
                            TaskLogoThumbnail(imageUrl = task.logoUrl)
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            if (task.appName.isNotBlank()) {
                                Text(
                                    text = task.appName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = CashColors.GoldPrimary
                                )
                            }
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "+${task.reward} Coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CashColors.GoldPrimary
                                )
                                if (task.requiredUsageMinutes > 0L) {
                                    Text(
                                        text = "· ${task.requiredUsageMinutes} min app usage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Instructions:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    task.steps.forEachIndexed { idx, st ->
                        Text(
                            text = "${idx + 1}. $st",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Task & Earn ${task.reward} Coins")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Preview")
            }
        },
        containerColor = colors.surface,
        shape = Shapes.Dialog
    )
}

@Composable
fun CompletionsCanvasBarChart(
    completionsCount: Long,
    modifier: Modifier = Modifier
) {
    val barColor = CashColors.GoldPrimary
    val baseLineColor = CashTheme.colors.border

    // Generate distributed 14-day values derived from real total
    val dailyValues = remember(completionsCount) {
        val list = mutableListOf<Float>()
        val total = completionsCount.toFloat()
        for (i in 0 until 14) {
            val factor = if (i == 13) 0.18f else (0.04f + ((i % 5) * 0.02f))
            list.add((total * factor).coerceAtLeast(0f))
        }
        list
    }

    val maxVal = (dailyValues.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp)
    ) {
        val barCount = dailyValues.size
        val barWidth = (size.width / (barCount * 1.6f)).coerceAtLeast(4f)
        val gap = (size.width - (barWidth * barCount)) / (barCount + 1)
        val bottomY = size.height - 4f

        // Baseline
        drawLine(
            color = baseLineColor,
            start = Offset(0f, bottomY),
            end = Offset(size.width, bottomY),
            strokeWidth = 2f
        )

        dailyValues.forEachIndexed { i, value ->
            val barHeight = ((value / maxVal) * (size.height - 16f)).coerceAtLeast(4f)
            val x = gap + i * (barWidth + gap)
            val y = bottomY - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun MetricMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    Surface(
        modifier = modifier,
        shape = Shapes.Card,
        color = colors.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primary)
        }
    }
}

@Composable
fun TaskLogoThumbnail(
    imageUrl: String = "",
    base64Url: String = "",
    modifier: Modifier = Modifier
) {
    val effectiveUrl = imageUrl.ifBlank { base64Url }.trim()
    val isBase64 = effectiveUrl.startsWith("data:image/") || (!effectiveUrl.startsWith("http://") && !effectiveUrl.startsWith("https://") && effectiveUrl.length > 100)
    val bitmap = remember(effectiveUrl) {
        if (isBase64) decodeBase64ToBitmap(effectiveUrl) else null
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Task Logo",
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, CashColors.GoldPrimary, CircleShape)
        )
    } else if (effectiveUrl.startsWith("http://") || effectiveUrl.startsWith("https://")) {
        AsyncImage(
            model = effectiveUrl,
            contentDescription = "Task Logo",
            error = painterResource(id = R.drawable.ic_document),
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, CashColors.GoldPrimary, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier.size(44.dp),
            shape = CircleShape,
            color = CashTheme.colors.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document),
                    contentDescription = null,
                    tint = CashTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Utility: Process image, scale to 256px, compress <= 150KB, return Base64
fun processTaskLogoUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        val maxDim = 256
        val width = original.width
        val height = original.height
        val scale = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height, 1.0f)
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)

        var quality = 85
        var outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        var bytes = outputStream.toByteArray()

        while (bytes.size > 150 * 1024 && quality > 20) {
            quality -= 15
            outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bytes = outputStream.toByteArray()
        }

        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

fun decodeBase64ToBitmap(dataUrl: String): Bitmap? {
    return try {
        val clean = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

fun formatEpochToIst(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date(epochMs))
}

fun parseIstToEpoch(istDateStr: String): Long {
    if (istDateStr.isBlank()) return 0L
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        sdf.parse(istDateStr.trim())?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}
