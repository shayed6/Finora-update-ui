package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.OrganizationInfoRepository
import com.example.data.preferences.WorkProfileKey
import com.example.data.repository.IncomeExpenseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTypeScreen(
    onNavigateBack: () -> Unit,
    isFirstTimePrompt: Boolean = false,
    onComplete: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val orgRepo = remember { OrganizationInfoRepository.getInstance(context) }
    val incomeExpenseRepo = remember { IncomeExpenseRepository.getInstance(context) }

    val savedProfiles by orgRepo.selectedWorkProfiles.collectAsState(initial = null)

    var selectedSet by rememberSaveable { mutableStateOf(setOf<String>()) }
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedProfiles) {
        if (!isInitialized && savedProfiles != null) {
            selectedSet = savedProfiles!!
            isInitialized = true
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_work_type"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "আপনার কাজের ধরন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isFirstTimePrompt) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_work_type_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("work_type_bottom_bar"),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                orgRepo.saveWorkProfiles(selectedSet)
                                orgRepo.setWorkTypePromptShown(true)
                                incomeExpenseRepo.applyWorkProfilePresets(selectedSet)
                                Toast.makeText(
                                    context,
                                    "কাজের ধরন সংরক্ষিত হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_work_type"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "সংরক্ষণ করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isFirstTimePrompt) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    orgRepo.setWorkTypePromptShown(true)
                                    onComplete()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_skip_work_type"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "পরে করব",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "আপনার পেশা বা আয়ের ধরন নির্বাচন করুন। এটি আপনার প্রয়োজন অনুযায়ী আয় ও ব্যয়ের খাতা সাজাতে সহায়তা করবে (একাধিক নির্বাচন করা সম্ভব):",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Card 1: ব্যবসা (Shop icon)
            WorkTypeCard(
                title = "ব্যবসা",
                subtitle = "দোকান, পাইকারি বা খুচরা পণ্য ক্রয়-বিক্রয় এবং বাণিজ্যিক লেনদেন",
                icon = Icons.Default.Storefront,
                isSelected = selectedSet.contains(WorkProfileKey.BUSINESS),
                onToggle = {
                    selectedSet = if (selectedSet.contains(WorkProfileKey.BUSINESS)) {
                        selectedSet - WorkProfileKey.BUSINESS
                    } else {
                        selectedSet + WorkProfileKey.BUSINESS
                    }
                },
                modifier = Modifier.testTag("card_work_type_business")
            )

            // Card 2: চাকরি (Briefcase icon)
            WorkTypeCard(
                title = "চাকরি",
                subtitle = "মাসিক বেতন, প্রাতিষ্ঠানিক উৎসব ভাতা বা বোনাস এবং ওভারটাইম আয়",
                icon = Icons.Default.BusinessCenter,
                isSelected = selectedSet.contains(WorkProfileKey.JOB),
                onToggle = {
                    selectedSet = if (selectedSet.contains(WorkProfileKey.JOB)) {
                        selectedSet - WorkProfileKey.JOB
                    } else {
                        selectedSet + WorkProfileKey.JOB
                    }
                },
                modifier = Modifier.testTag("card_work_type_job")
            )

            // Card 3: ফ্রিল্যান্সিং (Laptop icon)
            WorkTypeCard(
                title = "ফ্রিল্যান্সিং",
                subtitle = "রিমোট কাজ, ক্লায়েন্ট প্রজেক্ট পেমেন্ট এবং সফটওয়্যার টুলসের খরচ",
                icon = Icons.Default.Laptop,
                isSelected = selectedSet.contains(WorkProfileKey.FREELANCE),
                onToggle = {
                    selectedSet = if (selectedSet.contains(WorkProfileKey.FREELANCE)) {
                        selectedSet - WorkProfileKey.FREELANCE
                    } else {
                        selectedSet + WorkProfileKey.FREELANCE
                    }
                },
                modifier = Modifier.testTag("card_work_type_freelance")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WorkTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "borderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
