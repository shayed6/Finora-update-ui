package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.util.AppConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinoraTopBar(
    title: String,
    subtitle: String? = null,
    canNavigateBack: Boolean = false,
    onNavigationClick: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                if (!canNavigateBack && title == AppConfig.APP_NAME) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FinoraLogo(size = 34.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        FinoraWordmark(
                            titleSize = 18.sp,
                            textColor = Color.White,
                            taglineColor = Color.White.copy(alpha = 0.75f),
                            taglineText = "ফাইন্যান্সিয়াল ক্যালকুলেটর",
                            showTagline = true
                        )
                    }
                } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigationClick,
                modifier = Modifier.testTag("top_bar_nav_button")
            ) {
                if (canNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান (Back)",
                        tint = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "মেনু খুলুন (Menu)",
                        tint = Color.White
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FinoraNavy,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.08f))
}
}
