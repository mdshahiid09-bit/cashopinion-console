package com.cashopinion.console.ui.screens.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.FaqItem
import com.cashopinion.console.data.model.PageItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.GoldPillButton
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun PagesFaqScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Legal & App Pages", "Help & FAQs")

    val pages by consoleRepo.observePages().collectAsState(initial = emptyList())
    val faqs by consoleRepo.observeFaqs().collectAsState(initial = emptyList())

    // Page editing state
    val pageKeys = listOf("about", "terms", "privacy", "refer_rules", "refer_terms")
    var selectedPageKey by remember { mutableStateOf("terms") }
    var pageTitle by remember { mutableStateOf("") }
    var pageBody by remember { mutableStateOf("") }
    var isSavingPage by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPageKey, pages) {
        val found = pages.find { it.id == selectedPageKey }
        if (found != null) {
            pageTitle = found.title
            pageBody = found.body
        } else {
            pageTitle = selectedPageKey.replace("_", " ").uppercase()
            pageBody = "<p>Content for $selectedPageKey</p>"
        }
    }

    // FAQ dialog
    var showFaqDialog by remember { mutableStateOf(false) }
    var faqQuestion by remember { mutableStateOf("") }
    var faqAnswer by remember { mutableStateOf("") }
    var faqOrder by remember { mutableStateOf("1") }
    var isSavingFaq by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
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

        if (selectedTab == 0) {
            // Pages Editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Document to Edit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pageKeys.take(3).forEach { key ->
                        FilterChip(
                            selected = selectedPageKey == key,
                            onClick = { selectedPageKey = key },
                            label = { Text(key.replace("_", " ").capitalize()) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pageKeys.drop(3).forEach { key ->
                        FilterChip(
                            selected = selectedPageKey == key,
                            onClick = { selectedPageKey = key },
                            label = { Text(key.replace("_", " ").capitalize()) }
                        )
                    }
                }

                LuxuryCard {
                    OutlinedTextField(
                        value = pageTitle,
                        onValueChange = { pageTitle = it },
                        label = { Text("Page Display Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pageBody,
                        onValueChange = { pageBody = it },
                        label = { Text("Content (HTML formatted text)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        maxLines = 10
                    )
                }

                GoldPillButton(
                    text = "Publish Page Updates",
                    onClick = {
                        isSavingPage = true
                        scope.launch {
                            consoleRepo.savePage(selectedPageKey, pageTitle.trim(), pageBody.trim())
                            SoundFx.playSuccess()
                            isSavingPage = false
                        }
                    },
                    isLoading = isSavingPage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // FAQs Manager
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldPillButton(
                    text = "+ Add Question & Answer",
                    onClick = { showFaqDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                if (faqs.isEmpty()) {
                    EmptyStateView(
                        iconRes = R.drawable.ic_info,
                        message = "No FAQs created yet."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(faqs, key = { it.id }) { faq ->
                            LuxuryCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${faq.order} ${faq.question}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = faq.active,
                                        onCheckedChange = { active ->
                                            scope.launch {
                                                consoleRepo.saveFaq(faq.copy(active = active))
                                                SoundFx.playClick()
                                            }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                consoleRepo.deleteFaq(faq.id)
                                                SoundFx.playWarning()
                                            }
                                        }
                                    ) {
                                        Text("Delete", color = colors.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Add New FAQ", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = faqQuestion,
                        onValueChange = { faqQuestion = it },
                        label = { Text("Question") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = faqAnswer,
                        onValueChange = { faqAnswer = it },
                        label = { Text("Answer Explanation") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = faqOrder,
                        onValueChange = { faqOrder = it },
                        label = { Text("Sorting Order (e.g. 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (faqQuestion.isNotBlank()) {
                            isSavingFaq = true
                            scope.launch {
                                val item = FaqItem(
                                    question = faqQuestion.trim(),
                                    answer = faqAnswer.trim(),
                                    order = faqOrder.toIntOrNull() ?: 1,
                                    active = true
                                )
                                consoleRepo.saveFaq(item)
                                SoundFx.playSuccess()
                                faqQuestion = ""
                                faqAnswer = ""
                                isSavingFaq = false
                                showFaqDialog = false
                            }
                        }
                    },
                    enabled = faqQuestion.isNotBlank() && !isSavingFaq,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSavingFaq) "Saving..." else "Save FAQ", color = Color(0xFF140E00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}
