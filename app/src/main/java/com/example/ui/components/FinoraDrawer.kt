package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondaryLight
import com.example.util.AppConfig

enum class DrawerDestination {
    HOME,
    PORTFOLIO,
    SETTINGS,
    ABOUT,
    PRIVACY_POLICY,
    LEARN_STOCK
}

@Composable
fun FinoraDrawerContent(
    currentDestination: DrawerDestination,
    onNavigate: (DrawerDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLearnStockExpanded by remember { mutableStateOf(true) }

    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ওয়েব লিংক খুলতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPlayStoreRate() {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${AppConfig.PLAY_STORE_PACKAGE_ID}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            openExternalUrl(AppConfig.PLAY_STORE_WEB_URL)
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            val isDark = MaterialTheme.colorScheme.surface == SurfaceDark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0F1B35) else FinoraNavy)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FinoraLogo(size = 50.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        FinoraWordmark(
                            titleSize = 22.sp,
                            textColor = Color.White,
                            taglineColor = GrowthGreen,
                            showTagline = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "বাংলাদেশী বিনিয়োগকারী ও সাধারণ মানুষের আর্থিক হিসাবের স্মার্ট সঙ্গী।",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val drawerItemColors = NavigationDrawerItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                unselectedContainerColor = Color.Transparent
            )

            // Navigation Items
            // 1. Home
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home Icon") },
                label = {
                    Text(
                        text = "হোম (Home)",
                        fontWeight = if (currentDestination == DrawerDestination.HOME) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentDestination == DrawerDestination.HOME,
                onClick = {
                    onNavigate(DrawerDestination.HOME)
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_home")
            )

            // Portfolio (Item right after Home as per Part B1)
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.PieChart, contentDescription = "Portfolio Icon") },
                label = {
                    Text(
                        text = "পোর্টফোলিও (Portfolio)",
                        fontWeight = if (currentDestination == DrawerDestination.PORTFOLIO) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentDestination == DrawerDestination.PORTFOLIO,
                onClick = {
                    onNavigate(DrawerDestination.PORTFOLIO)
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_portfolio")
            )

            // 2. Learn Stock (Expandable Section)
            val chevronRotation by animateFloatAsState(
                targetValue = if (isLearnStockExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "chevron_rotate"
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLearnStockExpanded = !isLearnStockExpanded }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Learn Stock Icon",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Learn Stock (শেয়ার বাজার শিখুন)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "শীর্ষস্থানীয় শিক্ষামূলক প্ল্যাটফর্ম",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation)
                    )
                }

                AnimatedVisibility(
                    visible = isLearnStockExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 8.dp)) {
                        LearningLinkItem(
                            title = "Investaloy",
                            subtitle = "শেয়ার অ্যানালাইসিস ও ফান্ডামেন্টাল",
                            onClick = {
                                openExternalUrl(AppConfig.URL_INVESTALOY)
                                onCloseDrawer()
                            }
                        )
                        LearningLinkItem(
                            title = "Investment mentor Kabir",
                            subtitle = "টেকনিক্যাল অ্যানালাইসিস ও স্ট্র্যাটেজি",
                            onClick = {
                                openExternalUrl(AppConfig.URL_INVESTMENT_MENTOR_KABIR)
                                onCloseDrawer()
                            }
                        )
                        LearningLinkItem(
                            title = "Finance with  Rafyq",
                            subtitle = "ডিএসই ফান্ডামেন্টাল ও গাইড",
                            onClick = {
                                openExternalUrl(AppConfig.URL_FINANCE_WITH_RAFYQ)
                                onCloseDrawer()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Settings
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings Icon") },
                label = {
                    Text(
                        text = "সেটিংস (Settings)",
                        fontWeight = if (currentDestination == DrawerDestination.SETTINGS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentDestination == DrawerDestination.SETTINGS,
                onClick = {
                    onNavigate(DrawerDestination.SETTINGS)
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_settings")
            )

            // 4. About
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = "About Icon") },
                label = {
                    Text(
                        text = "আমাদের সম্পর্কে (About)",
                        fontWeight = if (currentDestination == DrawerDestination.ABOUT) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentDestination == DrawerDestination.ABOUT,
                onClick = {
                    onNavigate(DrawerDestination.ABOUT)
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_about")
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            // 5. Rate Us
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Star, contentDescription = "Rate Icon", tint = MaterialTheme.colorScheme.primary) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "রেটিং দিন (Rate Us)", modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                selected = false,
                onClick = {
                    openPlayStoreRate()
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_rate")
            )

            // 6. Privacy Policy
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy Icon") },
                label = {
                    Text(
                        text = "গোপনীয়তা নীতি (Privacy Policy)",
                        fontWeight = if (currentDestination == DrawerDestination.PRIVACY_POLICY) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentDestination == DrawerDestination.PRIVACY_POLICY,
                onClick = {
                    onNavigate(DrawerDestination.PRIVACY_POLICY)
                    onCloseDrawer()
                },
                colors = drawerItemColors,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_privacy")
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Footer info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Finora সংস্করণ ${AppConfig.APP_VERSION}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "১০০% অফলাইন এবং অন-ডিভাইস হিসাব",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrowthGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LearningLinkItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(GrowthGreen)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open Link",
            tint = PrimaryBlue,
            modifier = Modifier.size(16.dp)
        )
    }
}
