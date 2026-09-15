package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoritesManager
import com.example.data.preferences.AppLanguage
import com.example.model.CalculatorCategory
import com.example.model.CalculatorDef
import com.example.model.CalculatorRepository
import com.example.ui.LocalAppLanguage
import com.example.ui.components.CalculatorListItem
import com.example.ui.components.UnlockFavoriteSlotsDialog
import com.example.ui.theme.GrowthGreenDark
import com.example.ui.theme.GrowthGreenLight
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter

@Composable
fun CategoryDetailScreen(
    category: CalculatorCategory,
    onCalculatorClick: (CalculatorDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val calculators = CalculatorRepository.getByCategory(category)
    val favoriteIds by FavoritesManager.favorites.collectAsState()
    val maxSlots by FavoritesManager.maxSlots.collectAsState()
    var showUnlockSlotsDialog by remember { mutableStateOf(false) }

    val handleToggleFavorite: (String) -> Unit = { calcId ->
        val success = FavoritesManager.toggleFavorite(calcId)
        if (!success) {
            showUnlockSlotsDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isGrowthCategory = category == CalculatorCategory.SIP_INVESTMENT || category == CalculatorCategory.STOCK_AVG_PL
                val headerIconTint = if (isGrowthCategory) GrowthGreenDark else PrimaryBlue
                val headerBadgeBg = if (isGrowthCategory) GrowthGreenLight else PrimaryBlue.copy(alpha = 0.12f)
                val countBadgeBg = if (isGrowthCategory) GrowthGreenLight else PrimaryBlue.copy(alpha = 0.12f)
                val countBadgeTextColor = if (isGrowthCategory) GrowthGreenDark else PrimaryBlue

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(headerBadgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = headerIconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) category.titleEn else category.titleBn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEnglish) category.titleBn else category.descriptionBn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = countBadgeBg
                ) {
                    Text(
                        text = if (isEnglish) "${calculators.size}" else "${BengaliFormatter.toBengaliDigits(calculators.size.toString())} টি",
                        color = countBadgeTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // List of Calculators
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(calculators, key = { it.id }) { calc ->
                CalculatorListItem(
                    calculator = calc,
                    onClick = { onCalculatorClick(calc) },
                    isFavorite = favoriteIds.contains(calc.id),
                    onToggleFavorite = { handleToggleFavorite(calc.id) }
                )
            }
        }
    }

    if (showUnlockSlotsDialog) {
        UnlockFavoriteSlotsDialog(
            currentSlots = favoriteIds.size,
            maxSlots = maxSlots,
            onDismiss = { showUnlockSlotsDialog = false }
        )
    }
}
