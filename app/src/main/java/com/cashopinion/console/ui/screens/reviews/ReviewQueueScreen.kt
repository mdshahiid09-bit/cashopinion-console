package com.cashopinion.console.ui.screens.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.ReviewItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewQueueScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allReviews by consoleRepo.observeReviewQueue().collectAsState(initial = emptyList())

    val pendingReviews = remember(allReviews) {
        allReviews.filter { it.status == "pending" }
    }

    var selectedReviewForAction by remember { mutableStateOf<ReviewItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "approve" | "reject"
    var rejectReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    text = "Proof & Task Review Queue",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${pendingReviews.size} Submissions Pending (Includes proof & optional submissions)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingReviews.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_check_circle,
                message = "Review queue is clean! No pending submissions."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingReviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        onApprove = {
                            SoundFx.playClick()
                            selectedReviewForAction = review
                            actionType = "approve"
                        },
                        onReject = {
                            SoundFx.playClick()
                            selectedReviewForAction = review
                            actionType = "reject"
                            rejectReason = ""
                        }
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (selectedReviewForAction != null && actionType != null) {
        val review = selectedReviewForAction!!
        val isProofEmpty = review.proof.isBlank() || review.proof.equals("none", true) || review.proof.equals("optional", true)

        AlertDialog(
            onDismissRequest = {
                if (!isProcessing) {
                    selectedReviewForAction = null
                    actionType = null
                }
            },
            title = {
                Text(
                    text = if (actionType == "approve") "Approve Submission & Award Coins" else "Reject Submission",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User UID: ${review.uid}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Task: ${review.taskTitle.ifBlank { review.taskId }}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    Text("Proof Submission Status:", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Surface(
                        shape = Shapes.Chip,
                        color = if (isProofEmpty) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isProofEmpty) "Proof Optional: Submitted directly without mandatory file/text proof." else review.proof,
                            color = if (isProofEmpty) colors.primary else colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    if (actionType == "reject") {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Rejection Reason (e.g. Terms not met)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            if (actionType == "approve") {
                                consoleRepo.approveReview(review)
                                SoundFx.playSuccess()
                            } else {
                                consoleRepo.rejectReview(review, rejectReason)
                                SoundFx.playWarning()
                            }
                            isProcessing = false
                            selectedReviewForAction = null
                            actionType = null
                        }
                    },
                    enabled = !isProcessing && (actionType == "approve" || rejectReason.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "approve") colors.success else colors.error
                    )
                ) {
                    Text(if (isProcessing) "Processing..." else "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedReviewForAction = null
                        actionType = null
                    },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

@Composable
fun ReviewCard(
    review: ReviewItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val colors = CashTheme.colors
    val now = System.currentTimeMillis()
    val isOlderThan48h = review.submittedAt > 0 && (now - review.submittedAt) > (48 * 3600 * 1000L)
    val isProofEmpty = review.proof.isBlank() || review.proof.equals("none", true) || review.proof.equals("optional", true)

    val dateStr = remember(review.submittedAt) {
        if (review.submittedAt > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(review.submittedAt))
        } else "N/A"
    }

    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOlderThan48h) colors.error else colors.border
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.taskTitle.ifBlank { "Task: ${review.taskId}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (isOlderThan48h) {
                    Surface(
                        shape = Shapes.Chip,
                        color = colors.error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ">48h SLA BREACH",
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("User: ${review.uid}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text("Submitted: $dateStr", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = Shapes.Chip,
                color = if (isProofEmpty) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isProofEmpty) "Proof: Optional (No proof required)" else "Proof Submitted (${review.proofType}):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isProofEmpty) colors.primary else colors.textPrimary
                        )

                        if (isProofEmpty) {
                            Surface(
                                shape = Shapes.Chip,
                                color = colors.primary.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "PROOF OPTIONAL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = colors.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isProofEmpty) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.proof,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                    } else if (review.trackedUsageMs > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tracked In-App Usage: ${review.trackedUsageMs / 1000}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve & Award")
                }

                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}
