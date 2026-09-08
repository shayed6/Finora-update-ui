package com.example.ui.screens.goals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingsGoalsScreen(
    useBengaliDigits: Boolean = true,
    viewModel: SavingsGoalsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goals by viewModel.goals.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var goalForContribution by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GrowthGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("goals_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ব্যক্তিগত সঞ্চয় লক্ষ্য (Savings Goals)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "নির্দিষ্ট লক্ষ্য নির্ধারণ করুন ও অগ্রগতির ভিজ্যুয়াল হিসাব রাখুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { exportSavingsReport(context, goals, useBengaliDigits) },
                        modifier = Modifier.testTag("export_report_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Report",
                            tint = PrimaryBlue
                        )
                    }
                }
            }

            // Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goals_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GrowthGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = GrowthGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "মোট সঞ্চয় অগ্রগতি",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${BengaliFormatter.formatNumber(goals.size.toDouble(), 0, useBengaliDigits)}টি লক্ষ্য",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "এখন পর্যন্ত মোট সঞ্চিত",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(totalSaved, useBengaliDigits),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = GrowthGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "মোট সঞ্চয় লক্ষ্য",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(totalTarget, useBengaliDigits),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue = overallProgress,
                            animationSpec = tween(durationMillis = 600),
                            label = "overall_progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GrowthGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val percent = if (totalTarget > 0) (totalSaved / totalTarget) * 100.0 else 0.0
                        Text(
                            text = "সামগ্রিক অগ্রগতি: ${BengaliFormatter.formatPercent(percent, useBengaliDigits)}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "রিপোর্ট তৈরি ও শেয়ার",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    exportSavingsReport(context, goals, useBengaliDigits)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue.copy(alpha = 0.12f),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("export_goals_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Report",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "রিপোর্ট এক্সপোর্ট করুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (goals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GrowthGreen.copy(alpha = 0.12f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = GrowthGreen,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "এখনো কোনো সঞ্চয় লক্ষ্য যোগ করা হয়নি",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "জরুরী তহবিল, বাড়ি বুকিং, হজ্ব, বা সন্তানের ভবিষ্যৎ উচ্চশিক্ষার মতো যে কোনো ব্যক্তিগত সঞ্চয় লক্ষ্য যুক্ত করুন।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("লক্ষ্য যোগ করুন")
                            }
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalItemCard(
                        goal = goal,
                        useBengaliDigits = useBengaliDigits,
                        onAddContribution = { goalForContribution = goal },
                        onDelete = { goalToDelete = goal }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, target, initial, targetDate, category ->
                viewModel.addGoal(title, target, initial, targetDate, category)
                showAddDialog = false
            }
        )
    }

    // Add Contribution Dialog
    goalForContribution?.let { goal ->
        AddContributionDialog(
            goal = goal,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { goalForContribution = null },
            onAdd = { amount ->
                viewModel.addContribution(goal.id, amount)
                goalForContribution = null
            }
        )
    }

    // Delete Goal Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = {
                Text(
                    text = "লক্ষ্যটি মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে \"${goal.title}\" লক্ষ্যটি তালিকা থেকে মুছে ফেলতে চান?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { goalToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun GoalItemCard(
    goal: SavingsGoalEntity,
    useBengaliDigits: Boolean,
    onAddContribution: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100.0 else 0.0
    val isCompleted = goal.currentAmount >= goal.targetAmount
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "goal_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCompleted) GrowthGreen.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = goal.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) GrowthGreen else PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = goal.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = GrowthGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = AlertRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "বর্তমান জমা",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BengaliFormatter.formatTaka(goal.currentAmount, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isCompleted) GrowthGreen else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "লক্ষ্যমাত্রা",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BengaliFormatter.formatTaka(goal.targetAmount, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) GrowthGreen else PrimaryBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) "লক্ষ্য অর্জিত হয়েছে! 🎉" else "বাকি: ${BengaliFormatter.formatTaka(remaining, useBengaliDigits)} (${goal.targetDate})",
                    fontSize = 11.5.sp,
                    color = if (isCompleted) GrowthGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAddContribution)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "জমা যোগ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, initial: Double, targetDate: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("২০২৬") }
    var category by remember { mutableStateOf("জরুরী") }

    val presetCategories = listOf("জরুরী", "শিক্ষা", "সম্পদ", "ভ্রমণ", "অবসর")

    val isValid = title.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন সঞ্চয় লক্ষ্য যোগ করুন",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GrowthGreen.copy(alpha = 0.15f),
                                selectedLabelColor = GrowthGreen
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("লক্ষ্যের নাম (যেমন: জরুরী তহবিল)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("মোট লক্ষ্যমাত্রা (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("বর্তমান প্রাথমিক জমা (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("লক্ষ্য অর্জনের সময়সীমা (যেমন: ডিসেম্বর ২০২৬)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val initial = initialText.toDoubleOrNull() ?: 0.0
                    if (isValid) {
                        onSave(title, target, initial, targetDate, category)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen)
            ) {
                Text("সেভ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun AddContributionDialog(
    goal: SavingsGoalEntity,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${goal.title}-এ জমা যোগ করুন",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "বর্তমানে সঞ্চিত: ${BengaliFormatter.formatTaka(goal.currentAmount, useBengaliDigits)} (লক্ষ্য: ${BengaliFormatter.formatTaka(goal.targetAmount, useBengaliDigits)})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("নতুন জমার পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (amount > 0) onAdd(amount) },
                enabled = amount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen)
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * Exports the tracked financial goals and savings data as a simple formatted text report.
 * Launches the Android system share sheet and copies the report to the clipboard.
 */
fun exportSavingsReport(context: Context, goals: List<SavingsGoalEntity>, useBengaliDigits: Boolean) {
    if (goals.isEmpty()) {
        Toast.makeText(context, "কোনো সঞ্চয় লক্ষ্য যুক্ত করা নেই। প্রথমে একটি লক্ষ্য যোগ করুন।", Toast.LENGTH_SHORT).show()
        return
    }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val remaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
    val overallPercent = if (totalTarget > 0) (totalSaved / totalTarget) * 100.0 else 0.0
    val completedCount = goals.count { it.currentAmount >= it.targetAmount }
    val activeCount = goals.size - completedCount

    val dateStr = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(Date())

    val report = buildString {
        appendLine("==========================================")
        appendLine("           FINORA FINANCIAL REPORT        ")
        appendLine("   ব্যক্তিগত সঞ্চয় ও আর্থিক লক্ষ্য রিপোর্ট   ")
        appendLine("==========================================")
        appendLine("তারিখ ও সময়: $dateStr")
        appendLine()
        appendLine("■ সার্বিক সঞ্চয় পরিস্থিতি (Savings Overview)")
        appendLine("------------------------------------------")
        appendLine("• মোট লক্ষ্যমাত্রা (Target): ${BengaliFormatter.formatTaka(totalTarget, useBengaliDigits)}")
        appendLine("• মোট সঞ্চিত অর্থ (Saved):  ${BengaliFormatter.formatTaka(totalSaved, useBengaliDigits)}")
        appendLine("• বাকি সঞ্চয় (Remaining):   ${BengaliFormatter.formatTaka(remaining, useBengaliDigits)}")
        appendLine("• সামগ্রিক অগ্রগতি (Progress): ${BengaliFormatter.formatPercent(overallPercent, useBengaliDigits)}")
        appendLine("• মোট লক্ষ্য সংখ্যা:        ${BengaliFormatter.toBengaliDigits(goals.size.toString())}টি (${BengaliFormatter.toBengaliDigits(completedCount.toString())}টি সম্পন্ন, ${BengaliFormatter.toBengaliDigits(activeCount.toString())}টি চলমান)")
        appendLine()
        appendLine("■ লক্ষ্যসমূহের বিস্তারিত বিবরণ (Detailed Goals)")
        appendLine("------------------------------------------")

        goals.forEachIndexed { index, goal ->
            val goalProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100.0 else 0.0
            val goalRemaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            val isDone = goal.currentAmount >= goal.targetAmount
            val status = if (isDone) "অর্জিত হয়েছে (Completed) 🎉" else "চলমান (In Progress)"

            val num = BengaliFormatter.toBengaliDigits((index + 1).toString())
            appendLine("$num. ${goal.title} [ক্যাটাগরি: ${goal.category}]")
            appendLine("   - টার্গেট পরিমাণ:  ${BengaliFormatter.formatTaka(goal.targetAmount, useBengaliDigits)}")
            appendLine("   - বর্তমান সঞ্চয়:   ${BengaliFormatter.formatTaka(goal.currentAmount, useBengaliDigits)} (${BengaliFormatter.formatPercent(goalProgress, useBengaliDigits)})")
            if (!isDone) {
                appendLine("   - অবশিষ্ট প্রয়োজন: ${BengaliFormatter.formatTaka(goalRemaining, useBengaliDigits)}")
            }
            appendLine("   - অর্জনের টার্গেট তারিখ: ${goal.targetDate}")
            appendLine("   - অবস্থা: $status")
            appendLine()
        }

        appendLine("==========================================")
        appendLine("Finora — পার্সোনাল ফাইন্যান্স ও ইনভেস্টমেন্ট অ্যাপ")
        appendLine("১০০% অন-ডিভাইস নিরাপদ আর্থিক হিসাব")
        appendLine("==========================================")
    }

    try {
        // Copy to system clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Finora Savings Report", report)
        clipboard?.setPrimaryClip(clip)

        // Launch system share sheet
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "Finora সঞ্চয় লক্ষ্য রিপোর্ট - $dateStr")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "সঞ্চয় লক্ষ্য রিপোর্ট শেয়ার করুন").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
        Toast.makeText(context, "রিপোর্ট তৈরি হয়েছে ও ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "রিপোর্ট শেয়ার করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
    }
}
