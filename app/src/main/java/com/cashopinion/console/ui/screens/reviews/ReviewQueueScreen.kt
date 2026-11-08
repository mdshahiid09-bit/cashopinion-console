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
import com.cashopinion.console.R
import com.cashopinion.console.data.model.ReviewItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
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
                    text = "Review Queue",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${pendingReviews.size} Submissions Pending",
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
                            selectedReviewForAction = review
                            actionType = "approve"
                        },
                        onReject = {
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
        AlertDialog(
            onDismissRequest = {
                if (!isProcessing) {
                    selectedReviewForAction = null
                    actionType = null
                }
            },
            title = {
                Text(
                    text = if (actionType == "approve") "Approve Submission" else "Reject Submission",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("User: ${review.uid}", color = colors.textSecondary)
                    Text("Task ID: ${review.taskId}", color = colors.textSecondary)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Proof Provided:", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(text = review.proof, color = colors.primary)

                    if (actionType == "reject") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Rejection Reason") },
                            modifier = Modifier.fillMaxWidth()
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
                                consoleRepo.serverClient.approveReview(review.id)
                            } else {
                                consoleRepo.serverClient.rejectReview(review.id, rejectReason)
                            }
                            isProcessing = false
                            selectedReviewForAction = null
                            actionType = null
                        }
                    },
                    enabled = !isProcessing && (actionType == "approve" || rejectReason.isNotBlank())
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
                    text = "Task: ${review.taskId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
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
                color = colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Proof: ${review.proof}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(10.dp)
                )
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
