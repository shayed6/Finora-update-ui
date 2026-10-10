package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.OrganizationInfoRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationInfoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OrganizationInfoRepository.getInstance(context) }
    val savedInfo by repo.organizationInfo.collectAsState(initial = null)

    var orgName by rememberSaveable { mutableStateOf("") }
    var orgAddress by rememberSaveable { mutableStateOf("") }
    var orgPhone by rememberSaveable { mutableStateOf("") }
    var invoicePrefix by rememberSaveable { mutableStateOf("INV-") }
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedInfo) {
        if (!isInitialized && savedInfo != null) {
            val info = savedInfo!!
            orgName = info.name
            orgAddress = info.address
            orgPhone = info.phone
            invoicePrefix = info.invoicePrefix.ifBlank { "INV-" }
            isInitialized = true
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_organization_info"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "প্রতিষ্ঠানের তথ্য",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_org_info_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
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
                    .testTag("org_info_bottom_bar"),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                repo.saveOrganizationInfo(
                                    name = orgName,
                                    address = orgAddress,
                                    phone = orgPhone,
                                    invoicePrefix = invoicePrefix
                                )
                                Toast.makeText(
                                    context,
                                    "প্রতিষ্ঠানের তথ্য সফলভাবে সংরক্ষিত হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_org_info"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "সংরক্ষণ করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. প্রতিষ্ঠানের নাম (max 60)
            OutlinedTextField(
                value = orgName,
                onValueChange = { if (it.length <= 60) orgName = it },
                label = { Text("প্রতিষ্ঠানের নাম") },
                placeholder = { Text("যেমন: শাকিল এন্টারপ্রাইজ") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                supportingText = {
                    Text(
                        text = "পরে PDF রিপোর্ট ও ইনভয়েসে এই নামটি ব্যবহার হবে।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_org_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // 2. ঠিকানা (optional, max 120)
            OutlinedTextField(
                value = orgAddress,
                onValueChange = { if (it.length <= 120) orgAddress = it },
                label = { Text("ঠিকানা (ঐচ্ছিক)") },
                placeholder = { Text("যেমন: রোড-১২, ধানমন্ডি, ঢাকা") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_org_address"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // 3. ফোন (optional, max 20, accept Bengali or English digits)
            OutlinedTextField(
                value = orgPhone,
                onValueChange = { if (it.length <= 20) orgPhone = it },
                label = { Text("ফোন নম্বর (ঐচ্ছিক)") },
                placeholder = { Text("যেমন: ০১৭XXXXXXXX") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_org_phone"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // 4. ইনভয়েস নম্বরের প্রিফিক্স (optional, max 8, default "INV-")
            OutlinedTextField(
                value = invoicePrefix,
                onValueChange = { if (it.length <= 8) invoicePrefix = it },
                label = { Text("ইনভয়েস নম্বরের প্রিফিক্স (ঐচ্ছিক)") },
                placeholder = { Text("INV-") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_invoice_prefix"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
