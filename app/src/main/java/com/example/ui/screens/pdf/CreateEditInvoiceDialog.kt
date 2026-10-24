package com.example.ui.screens.pdf

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceWithItems
import com.example.data.local.entity.LedgerPartyEntity
import com.example.data.preferences.OrganizationInfo
import com.example.ui.components.FinoraDatePickerDialog
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import com.example.util.InvoiceCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DraftInvoiceItem(
    val id: Long = 0L,
    var name: String = "",
    var description: String = "",
    var quantityStr: String = "1",
    var unitPriceStr: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditInvoiceDialog(
    existingInvoice: InvoiceWithItems? = null,
    orgInfo: OrganizationInfo,
    existingParties: List<LedgerPartyEntity>,
    suggestedInvoiceNumber: String,
    onDismiss: () -> Unit,
    onSaveInvoice: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
) {
    var invoiceNumber by remember {
        mutableStateOf(existingInvoice?.invoice.let { it?.invoiceNumber } ?: suggestedInvoiceNumber)
    }

    var businessName by remember {
        mutableStateOf(existingInvoice?.invoice?.businessName ?: orgInfo.name.ifBlank { "Finora Business" })
    }
    var businessPhone by remember {
        mutableStateOf(existingInvoice?.invoice?.businessPhone ?: orgInfo.phone)
    }
    var businessAddress by remember {
        mutableStateOf(existingInvoice?.invoice?.businessAddress ?: orgInfo.address)
    }

    var customerName by remember { mutableStateOf(existingInvoice?.invoice?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(existingInvoice?.invoice?.customerPhone ?: "") }
    var customerAddress by remember { mutableStateOf(existingInvoice?.invoice?.customerAddress ?: "") }

    var invoiceDate by remember { mutableLongStateOf(existingInvoice?.invoice?.invoiceDate ?: System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf(existingInvoice?.invoice?.dueDate) }

    var discountInput by remember {
        mutableStateOf(
            existingInvoice?.invoice?.discountPaisa?.let { if (it > 0) "${it / 100.0}" else "" } ?: ""
        )
    }
    var chargesInput by remember {
        mutableStateOf(
            existingInvoice?.invoice?.additionalChargesPaisa?.let { if (it > 0) "${it / 100.0}" else "" } ?: ""
        )
    }
    var paidInput by remember {
        mutableStateOf(
            existingInvoice?.invoice?.paidAmountPaisa?.let { if (it > 0) "${it / 100.0}" else "" } ?: ""
        )
    }

    var paymentMethod by remember { mutableStateOf(existingInvoice?.invoice?.paymentMethod ?: "নগদ (Cash)") }
    var notes by remember { mutableStateOf(existingInvoice?.invoice?.notes ?: "") }
    var terms by remember { mutableStateOf(existingInvoice?.invoice?.terms ?: "") }
    var language by remember { mutableStateOf(existingInvoice?.invoice?.language ?: "BN") }

    val draftItems = remember {
        mutableStateListOf<DraftInvoiceItem>().apply {
            if (existingInvoice != null && existingInvoice.items.isNotEmpty()) {
                addAll(
                    existingInvoice.items.map {
                        DraftInvoiceItem(
                            id = it.id,
                            name = it.itemName,
                            description = it.description ?: "",
                            quantityStr = if (it.quantity % 1.0 == 0.0) "${it.quantity.toInt()}" else "${it.quantity}",
                            unitPriceStr = "${it.unitPricePaisa / 100.0}"
                        )
                    }
                )
            } else {
                add(DraftInvoiceItem(name = "", quantityStr = "1", unitPriceStr = ""))
            }
        }
    }

    var showInvoiceDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showPartyPicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Calculation derived state
    val calculationResult by remember {
        derivedStateOf {
            val entities = draftItems.mapIndexed { index, draft ->
                val qty = draft.quantityStr.toDoubleOrNull() ?: 1.0
                val unitPaisa = InvoiceCalculator.parseTakaToPaisa(draft.unitPriceStr)
                val totalPaisa = InvoiceCalculator.calculateItemTotalPaisa(qty, unitPaisa)
                InvoiceItemEntity(
                    id = draft.id,
                    invoiceId = existingInvoice?.invoice?.id ?: 0L,
                    itemName = draft.name,
                    description = draft.description.ifBlank { null },
                    quantity = qty,
                    unitPricePaisa = unitPaisa,
                    totalPricePaisa = totalPaisa,
                    sortOrder = index
                )
            }
            val discountPaisa = InvoiceCalculator.parseTakaToPaisa(discountInput)
            val chargesPaisa = InvoiceCalculator.parseTakaToPaisa(chargesInput)
            val paidPaisa = InvoiceCalculator.parseTakaToPaisa(paidInput)

            InvoiceCalculator.calculateInvoiceTotals(
                items = entities,
                discountPaisa = discountPaisa,
                additionalChargesPaisa = chargesPaisa,
                paidAmountPaisa = paidPaisa
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dialog_create_edit_invoice"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_invoice_dialog")) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }

                        Text(
                            text = if (existingInvoice != null) "ইনভয়েস সম্পাদনা" else "নতুন ইনভয়েস তৈরি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        // Language Toggle
                        FilterChip(
                            selected = language == "BN",
                            onClick = { language = if (language == "BN") "EN" else "BN" },
                            label = { Text(if (language == "BN") "বাংলা" else "English") },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Invoice Number and Dates Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ইনভয়েস পরিচিতি ও তারিখ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryBlue
                            )

                            OutlinedTextField(
                                value = invoiceNumber,
                                onValueChange = { invoiceNumber = it },
                                label = { Text("ইনভয়েস নম্বর (Invoice #)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_invoice_number")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Issue Date
                                OutlinedTextField(
                                    value = BengaliFormatter.formatBengaliDate(invoiceDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("ইস্যু তারিখ") },
                                    trailingIcon = {
                                        IconButton(onClick = { showInvoiceDatePicker = true }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showInvoiceDatePicker = true }
                                )

                                // Due Date
                                val dueStr = dueDate?.let { BengaliFormatter.formatBengaliDate(it) } ?: "মেয়াদ নেই"
                                OutlinedTextField(
                                    value = dueStr,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("পরিশোধের মেয়াদ") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDueDatePicker = true }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick due date")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showDueDatePicker = true }
                                )
                            }
                        }
                    }

                    // Customer / Client Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "গ্রাহক / কাস্টমার তথ্য",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryBlue
                                )

                                if (existingParties.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showPartyPicker = !showPartyPicker },
                                        modifier = Modifier.testTag("btn_pick_party")
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("বাকির খাতা থেকে নির্বাচন")
                                    }
                                }
                            }

                            if (showPartyPicker && existingParties.isNotEmpty()) {
                                Text(
                                    text = "গ্রাহক নির্বাচন করুন:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(existingParties) { party ->
                                        FilterChip(
                                            selected = customerName == party.name,
                                            onClick = {
                                                customerName = party.name
                                                party.phone?.let { customerPhone = it }
                                                showPartyPicker = false
                                            },
                                            label = { Text(party.name) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("গ্রাহকের নাম (Customer Name) *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_customer_name")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("ফোন নম্বর") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    label = { Text("ঠিকানা") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Line Items Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "পণ্য বা সেবার বিবরণী (Line Items)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryBlue
                                )

                                TextButton(
                                    onClick = {
                                        draftItems.add(DraftInvoiceItem(quantityStr = "1"))
                                    },
                                    modifier = Modifier.testTag("btn_add_line_item")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("আইটেম যোগ")
                                }
                            }

                            draftItems.forEachIndexed { index, item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "আইটেম #${index + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (draftItems.size > 1) {
                                                IconButton(
                                                    onClick = { draftItems.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete item",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = {
                                                item.name = it
                                                draftItems[index] = item.copy(name = it)
                                            },
                                            label = { Text("পণ্যের নাম / বিবরণ *") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = item.quantityStr,
                                                onValueChange = {
                                                    item.quantityStr = it
                                                    draftItems[index] = item.copy(quantityStr = it)
                                                },
                                                label = { Text("পরিমাণ (Qty)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = item.unitPriceStr,
                                                onValueChange = {
                                                    item.unitPriceStr = it
                                                    draftItems[index] = item.copy(unitPriceStr = it)
                                                },
                                                label = { Text("একক দর (৳)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1.5f)
                                            )

                                            val lineQty = item.quantityStr.toDoubleOrNull() ?: 0.0
                                            val linePrice = InvoiceCalculator.parseTakaToPaisa(item.unitPriceStr)
                                            val lineTotal = InvoiceCalculator.calculateItemTotalPaisa(lineQty, linePrice)

                                            Column(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .padding(top = 8.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(text = "লাইন মোট:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = BengaliFormatter.formatTaka(lineTotal / 100.0),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = PrimaryBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Totals & Modifiers Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "হিসাব ও মোট পরিশোধ (Calculations)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryBlue
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    label = { Text("ছাড় / Discount (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = chargesInput,
                                    onValueChange = { chargesInput = it },
                                    label = { Text("ভ্যাট / খরচ (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = paidInput,
                                onValueChange = { paidInput = it },
                                label = { Text("জমা / পরিশোধিত টাকা (৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Calculation Breakdown Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("উপ-মোট (Subtotal):", fontSize = 13.sp)
                                        Text(BengaliFormatter.formatTaka(calculationResult.subtotalPaisa / 100.0), fontWeight = FontWeight.SemiBold)
                                    }

                                    if (calculationResult.discountPaisa > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ছাড় (Discount):", fontSize = 13.sp, color = GrowthGreen)
                                            Text("- " + BengaliFormatter.formatTaka(calculationResult.discountPaisa / 100.0), color = GrowthGreen)
                                        }
                                    }

                                    if (calculationResult.additionalChargesPaisa > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("অতিরিক্ত চার্জ / ভ্যাট:", fontSize = 13.sp)
                                            Text("+ " + BengaliFormatter.formatTaka(calculationResult.additionalChargesPaisa / 100.0))
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("সর্বমোট (Grand Total):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(BengaliFormatter.formatTaka(calculationResult.grandTotalPaisa / 100.0), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("পরিশোধিত (Paid):", fontSize = 13.sp)
                                        Text(BengaliFormatter.formatTaka(calculationResult.paidAmountPaisa / 100.0), fontWeight = FontWeight.SemiBold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("অবশিষ্ট বকেয়া (Due):", fontWeight = FontWeight.Bold, color = if (calculationResult.dueAmountPaisa > 0) MaterialTheme.colorScheme.error else GrowthGreen)
                                        Text(
                                            text = BengaliFormatter.formatTaka(calculationResult.dueAmountPaisa / 100.0),
                                            fontWeight = FontWeight.Bold,
                                            color = if (calculationResult.dueAmountPaisa > 0) MaterialTheme.colorScheme.error else GrowthGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Payment Method & Notes
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "পেমেন্ট মাধ্যম ও শর্তাবলী",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryBlue
                            )

                            // Quick Payment Method Chips
                            val methods = listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)", "ব্যাংক (Bank)", "চেক (Cheque)")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(methods) { m ->
                                    FilterChip(
                                        selected = paymentMethod == m,
                                        onClick = { paymentMethod = m },
                                        label = { Text(m) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("নোট বা বিশেষ বার্তা (Notes)") },
                                placeholder = { Text("আমাদের সাথে লেনদেন করার জন্য ধন্যবাদ!") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = terms,
                                onValueChange = { terms = it },
                                label = { Text("শর্তাবলী (Terms & Conditions)") },
                                placeholder = { Text("মাল পৌঁছানোর ৭ দিনের মধ্যে কোনো আপত্তি থাকলে জানাতে হবে।") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Bottom Action Buttons Bar
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("বাতিল")
                        }

                        Button(
                            onClick = {
                                if (isSubmitting) return@Button
                                if (customerName.isBlank()) return@Button

                                isSubmitting = true
                                val entities = draftItems.filter { it.name.isNotBlank() }.mapIndexed { index, draft ->
                                    val qty = draft.quantityStr.toDoubleOrNull() ?: 1.0
                                    val unitPaisa = InvoiceCalculator.parseTakaToPaisa(draft.unitPriceStr)
                                    val totalPaisa = InvoiceCalculator.calculateItemTotalPaisa(qty, unitPaisa)
                                    InvoiceItemEntity(
                                        id = draft.id,
                                        invoiceId = existingInvoice?.invoice?.id ?: 0L,
                                        itemName = draft.name,
                                        description = draft.description.ifBlank { null },
                                        quantity = qty,
                                        unitPricePaisa = unitPaisa,
                                        totalPricePaisa = totalPaisa,
                                        sortOrder = index
                                    )
                                }

                                val invoiceEntity = InvoiceEntity(
                                    id = existingInvoice?.invoice?.id ?: 0L,
                                    invoiceNumber = invoiceNumber.ifBlank { "INV-0001" },
                                    businessName = businessName,
                                    businessPhone = businessPhone.ifBlank { null },
                                    businessAddress = businessAddress.ifBlank { null },
                                    customerName = customerName,
                                    customerPhone = customerPhone.ifBlank { null },
                                    customerAddress = customerAddress.ifBlank { null },
                                    invoiceDate = invoiceDate,
                                    dueDate = dueDate,
                                    subtotalPaisa = calculationResult.subtotalPaisa,
                                    discountPaisa = calculationResult.discountPaisa,
                                    additionalChargesPaisa = calculationResult.additionalChargesPaisa,
                                    grandTotalPaisa = calculationResult.grandTotalPaisa,
                                    paidAmountPaisa = calculationResult.paidAmountPaisa,
                                    dueAmountPaisa = calculationResult.dueAmountPaisa,
                                    paymentMethod = paymentMethod,
                                    notes = notes.ifBlank { null },
                                    terms = terms.ifBlank { null },
                                    status = calculationResult.status.name,
                                    language = language,
                                    createdAt = existingInvoice?.invoice?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )

                                onSaveInvoice(invoiceEntity, entities)
                                onDismiss()
                            },
                            enabled = !isSubmitting && customerName.isNotBlank() && draftItems.any { it.name.isNotBlank() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("btn_save_invoice_submit")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("সংরক্ষণ ও PDF তৈরি")
                        }
                    }
                }
            }
        }
    }

    if (showInvoiceDatePicker) {
        FinoraDatePickerDialog(
            initialDateMillis = invoiceDate,
            allowFutureDates = true,
            onDismissRequest = { showInvoiceDatePicker = false },
            onDateSelected = { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                invoiceDate = cal.timeInMillis
                showInvoiceDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        FinoraDatePickerDialog(
            initialDateMillis = dueDate ?: (System.currentTimeMillis() + 7 * 86400000L),
            allowFutureDates = true,
            onDismissRequest = { showDueDatePicker = false },
            onDateSelected = { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                dueDate = cal.timeInMillis
                showDueDatePicker = false
            }
        )
    }
}
