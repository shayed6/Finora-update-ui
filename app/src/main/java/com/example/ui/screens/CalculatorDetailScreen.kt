package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.model.CalcResult
import com.example.model.CalculatorDef
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedDark
import com.example.ui.theme.AlertRedDarkTheme
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.GrowthGreenDark
import com.example.ui.theme.GrowthGreenDarkTheme
import com.example.ui.theme.GrowthGreenLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TabularFigureStyle
import com.example.util.BengaliFormatter

@Composable
fun CalculatorDetailScreen(
    calculator: CalculatorDef,
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    val inputValues = remember(calculator.id) {
        mutableStateMapOf<String, String>().apply {
            calculator.inputs.forEach { input ->
                if (input.defaultValue.isNotEmpty()) {
                    this[input.id] = if (useBengaliDigits) {
                        BengaliFormatter.toBengaliDigits(input.defaultValue)
                    } else {
                        input.defaultValue
                    }
                }
            }
        }
    }

    val inputErrors = remember(calculator.id) { mutableStateMapOf<String, String>() }
    var result by remember(calculator.id) { mutableStateOf<CalcResult?>(null) }
    val scrollState = rememberScrollState()

    fun performCalculation() {
        inputErrors.clear()
        val numericMap = mutableMapOf<String, Double>()
        var hasError = false

        for (input in calculator.inputs) {
            val raw = inputValues[input.id]?.trim() ?: ""
            if (raw.isEmpty()) {
                if (input.isRequired) {
                    inputErrors[input.id] = "${input.labelBn} প্রদান করুন"
                    hasError = true
                } else {
                    numericMap[input.id] = 0.0
                }
            } else {
                val normalized = BengaliFormatter.normalizeToEnglishDigits(raw)
                val parsed = normalized.toDoubleOrNull()
                if (parsed == null) {
                    inputErrors[input.id] = "সঠিক সংখ্যা লিখুন (যেমন: ১২০ বা 120)"
                    hasError = true
                } else {
                    numericMap[input.id] = parsed
                }
            }
        }

        if (!hasError) {
            result = calculator.calculate(numericMap, useBengaliDigits)
        }
    }

    fun resetForm() {
        inputValues.clear()
        inputErrors.clear()
        calculator.inputs.forEach { input ->
            if (input.defaultValue.isNotEmpty()) {
                inputValues[input.id] = if (useBengaliDigits) {
                    BengaliFormatter.toBengaliDigits(input.defaultValue)
                } else {
                    input.defaultValue
                }
            }
        }
        result = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = calculator.category.icon,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = calculator.titleBn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = calculator.titleEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = calculator.descriptionBn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Formula Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সূত্র: ${calculator.formulaSummaryBn}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // If this is the SIP Calculator, render the dedicated wealth accumulation experience
        if (calculator.id == "sip_calc") {
            com.example.ui.components.SipCalculatorView(useBengaliDigits = useBengaliDigits)
        } else if (calculator.id == "inflation_calc") {
            com.example.ui.components.InflationCalculatorView(useBengaliDigits = useBengaliDigits)
        } else {
            // Form Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "প্রয়োজনীয় তথ্য ইনপুট দিন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                calculator.inputs.forEach { inputDef ->
                    val value = inputValues[inputDef.id] ?: ""
                    val error = inputErrors[inputDef.id]

                    Column {
                        Text(
                            text = inputDef.labelBn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                        )

                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                inputValues[inputDef.id] = newValue
                                if (inputErrors.containsKey(inputDef.id)) {
                                    inputErrors.remove(inputDef.id)
                                }
                            },
                            placeholder = {
                                Text(
                                    text = inputDef.placeholderBn,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.5.sp
                                )
                            },
                            trailingIcon = {
                                if (inputDef.unit.isNotEmpty()) {
                                    Text(
                                        text = inputDef.unit,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            },
                            isError = error != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(11.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_${inputDef.id}"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                errorBorderColor = AlertRed,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (error != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = error,
                                    color = AlertRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (inputDef.helpTextBn.isNotEmpty()) {
                            Text(
                                text = inputDef.helpTextBn,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                val calculateInteractionSource = remember { MutableInteractionSource() }
                val isCalculatePressed by calculateInteractionSource.collectIsPressedAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { resetForm() },
                        modifier = Modifier
                            .weight(0.35f)
                            .height(48.dp)
                            .testTag("reset_button"),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "রিসেট",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "রিসেট",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { performCalculation() },
                        interactionSource = calculateInteractionSource,
                        modifier = Modifier
                            .weight(0.65f)
                            .height(48.dp)
                            .testTag("calculate_button"),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCalculatePressed) PrimaryBlueDark else PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "হিসাব করুন",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "হিসাব করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Result Card
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn() + slideInVertically()
        ) {
            result?.let { res ->
                val isDark = MaterialTheme.colorScheme.surface == SurfaceDark
                val resultContainerColor = if (isDark) {
                    if (res.isWarning) AlertRed.copy(alpha = 0.15f) else GrowthGreen.copy(alpha = 0.15f)
                } else {
                    if (res.isWarning) AlertRedLight else GrowthGreenLight
                }
                val resultBorderColor = if (res.isWarning) AlertRed.copy(alpha = 0.6f) else GrowthGreen.copy(alpha = 0.6f)
                val primaryTextColor = if (isDark) {
                    if (res.isWarning) AlertRedDarkTheme else GrowthGreenDarkTheme
                } else {
                    if (res.isWarning) AlertRedDark else FinoraNavy
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = resultContainerColor
                    ),
                    border = BorderStroke(1.5.dp, resultBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = res.primaryLabelBn,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )

                            Icon(
                                imageVector = if (res.isWarning) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (res.isWarning) AlertRed else GrowthGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Primary Value (26sp bold, prominent) with tabular figures
                        Text(
                            text = res.primaryValueBn,
                            style = TabularFigureStyle.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryTextColor,
                                letterSpacing = 0.5.sp
                            )
                        )

                        // Sub-results
                        if (res.subResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                color = if (res.isWarning) AlertRed.copy(alpha = 0.2f) else GrowthGreen.copy(alpha = 0.25f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                res.subResults.forEach { subItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = subItem.labelBn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = subItem.valueBn,
                                            style = TabularFigureStyle.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Explanatory Note / Insight in supporting tone
                        Spacer(modifier = Modifier.height(14.dp))
                        val noteContainer = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.9f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(noteContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = if (res.isWarning) "⚠️" else "💡",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = res.insightNoteBn,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else if (res.isWarning) AlertRedDark else GrowthGreenDark,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
