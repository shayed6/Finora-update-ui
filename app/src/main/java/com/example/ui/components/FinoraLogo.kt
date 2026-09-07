package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.util.AppConfig

@Composable
fun FinoraLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val cornerRadius = size * 0.22f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(FinoraNavy)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_finora_logo),
            contentDescription = "Finora Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(cornerRadius))
        )
    }
}

/**
 * Modern Finora Wordmark matching the suggested branding:
 * "Finora" with green leaf accent on the 'i', plus "Your Money. Your Growth." tagline.
 */
@Composable
fun FinoraWordmark(
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    taglineColor: Color = Color.White.copy(alpha = 0.85f),
    titleSize: TextUnit = 22.sp,
    taglineText: String = AppConfig.TAGLINE_EN,
    showTagline: Boolean = true
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "F",
                color = textColor,
                fontSize = titleSize,
                fontWeight = FontWeight.Black
            )
            // 'i' with signature green leaf accent
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.padding(horizontal = 0.5.dp)
            ) {
                Text(
                    text = "ı", // dotless i
                    color = textColor,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black
                )
                // Distinctive green leaf as the tittle
                Box(
                    modifier = Modifier
                        .padding(top = (titleSize.value * 0.08f).dp, start = (titleSize.value * 0.08f).dp)
                        .size((titleSize.value * 0.24f).dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = (titleSize.value * 0.15f).dp,
                                topEnd = 0.dp,
                                bottomStart = 0.dp,
                                bottomEnd = (titleSize.value * 0.15f).dp
                            )
                        )
                        .background(GrowthGreen)
                )
            }
            Text(
                text = "nora",
                color = textColor,
                fontSize = titleSize,
                fontWeight = FontWeight.Black
            )
        }

        if (showTagline) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = taglineText,
                color = taglineColor,
                fontSize = (titleSize.value * 0.52f).sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}

