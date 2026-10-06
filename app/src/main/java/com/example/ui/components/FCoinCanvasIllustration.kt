package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * High-fidelity vector-based Coin Icon Illustration drawn using Compose Canvas.
 * Represents the Finora "F-Coin" with:
 * - Ambient golden radial glow
 * - Milled coin edge with radial serrations
 * - Multi-stop brushed metallic bevel rings and micro-bead border
 * - Subtle radial coin-die sunburst rays
 * - 3D embossed chiseled "F" monogram with Bengali Taka (৳) stylized serif
 * - Animated specular light glint / gleam sweep across the coin face
 * - Surrounding sparkling accent stars
 */
@Composable
fun FCoinCanvasIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fcoin_anim")

    // Subtle gentle floating / breathing scale
    val floatOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "coin_float"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Specular shine sweep position across the coin
    val shineProgress by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "coin_shine"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    // Star sparkle rotation & pulse
    val sparkleScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sparkle_pulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("fcoin_canvas_illustration"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
        ) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val coinCenter = Offset(canvasWidth / 2f, (canvasHeight / 2f) + floatOffset)
            val baseRadius = min(canvasWidth, canvasHeight) * 0.38f

            // 1. Ambient Golden Backing Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x66FFC107),
                        Color(0x33FF9800),
                        Color(0x10FFB300),
                        Color.Transparent
                    ),
                    center = coinCenter,
                    radius = baseRadius * 1.32f
                ),
                center = coinCenter,
                radius = baseRadius * 1.32f
            )

            // 2. Outer Milled Rim / Coin Serration Teeth (48 teeth)
            val teethCount = 48
            val outerTeethR = baseRadius
            val innerTeethR = baseRadius * 0.935f
            for (i in 0 until teethCount) {
                val angleRad = (i * (2f * PI / teethCount)).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val isHighlight = sinA < 0 || cosA < 0

                val pStart = Offset(
                    coinCenter.x + innerTeethR * cosA,
                    coinCenter.y + innerTeethR * sinA
                )
                val pEnd = Offset(
                    coinCenter.x + outerTeethR * cosA,
                    coinCenter.y + outerTeethR * sinA
                )

                drawLine(
                    color = if (isHighlight) Color(0xFFFFE57F) else Color(0xFF7A4500),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = baseRadius * 0.038f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Main Outer Rim Ring (Chiseled Gold Bevel)
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFF6D1), // Top-left brilliant specular gold
                    Color(0xFFFFD54F),
                    Color(0xFFFFB300),
                    Color(0xFFB26A00),
                    Color(0xFFFFE082),
                    Color(0xFF663800)  // Bottom-right shadow
                ),
                start = Offset(coinCenter.x - baseRadius, coinCenter.y - baseRadius),
                end = Offset(coinCenter.x + baseRadius, coinCenter.y + baseRadius)
            )

            drawCircle(
                brush = rimBrush,
                center = coinCenter,
                radius = baseRadius * 0.94f,
                style = Stroke(width = baseRadius * 0.08f)
            )

            // Inner recessed rim shadow groove
            drawCircle(
                color = Color(0x77422100),
                center = coinCenter,
                radius = baseRadius * 0.895f,
                style = Stroke(width = baseRadius * 0.018f)
            )

            // 4. Beaded Perimeter (32 Decorative Mint Pearls)
            val beadCount = 32
            val beadRingR = baseRadius * 0.84f
            val beadRadius = baseRadius * 0.024f
            for (i in 0 until beadCount) {
                val angleRad = (i * (2f * PI / beadCount)).toFloat()
                val bx = coinCenter.x + beadRingR * cos(angleRad)
                val by = coinCenter.y + beadRingR * sin(angleRad)

                // Bead shadow
                drawCircle(
                    color = Color(0x884A2800),
                    center = Offset(bx + 1.2f, by + 1.2f),
                    radius = beadRadius
                )
                // Bead highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF9E6), Color(0xFFFFB800), Color(0xFF8F4E00)),
                        center = Offset(bx - 0.8f, by - 0.8f),
                        radius = beadRadius * 1.3f
                    ),
                    center = Offset(bx, by),
                    radius = beadRadius
                )
            }

            // 5. Coin Face Die (Field of the coin)
            val coinFaceRadius = baseRadius * 0.79f
            val coinFaceBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF4B8), // Center warm bright highlight
                    Color(0xFFFFCC00),
                    Color(0xFFE58900),
                    Color(0xFFA65800)
                ),
                center = Offset(coinCenter.x - baseRadius * 0.22f, coinCenter.y - baseRadius * 0.22f),
                radius = coinFaceRadius * 1.35f
            )

            drawCircle(
                brush = coinFaceBrush,
                center = coinCenter,
                radius = coinFaceRadius
            )

            // Inner face subtle hairline ring
            drawCircle(
                color = Color(0x33FFFFFF),
                center = coinCenter,
                radius = coinFaceRadius * 0.94f,
                style = Stroke(width = baseRadius * 0.012f)
            )

            // 6. Subtle Sunburst Radiance Lines on Face (16 rays)
            val rayCount = 16
            for (i in 0 until rayCount) {
                val angleRad = (i * (2f * PI / rayCount)).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val r1 = coinFaceRadius * 0.28f
                val r2 = coinFaceRadius * 0.92f

                drawLine(
                    color = if (i % 2 == 0) Color(0x28FFFFFF) else Color(0x1A422200),
                    start = Offset(coinCenter.x + r1 * cosA, coinCenter.y + r1 * sinA),
                    end = Offset(coinCenter.x + r2 * cosA, coinCenter.y + r2 * sinA),
                    strokeWidth = baseRadius * 0.015f
                )
            }

            // 7. Central Finora "F" Monogram with 3D Embossing
            drawStylizedFMonogram(
                center = coinCenter,
                scale = baseRadius * 0.0175f
            )

            // 8. Dynamic Specular Light Glint Sweep across Coin Face
            if (animated) {
                val shineWidth = coinFaceRadius * 0.45f
                val shineX = coinCenter.x - coinFaceRadius + (shineProgress * coinFaceRadius * 2.2f)

                // Translucent sweep beam clipped to circle
                val shinePath = Path().apply {
                    addOval(
                        Rect(
                            left = coinCenter.x - coinFaceRadius,
                            top = coinCenter.y - coinFaceRadius,
                            right = coinCenter.x + coinFaceRadius,
                            bottom = coinCenter.y + coinFaceRadius
                        )
                    )
                }

                drawPath(
                    path = shinePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x10FFFFFF),
                            Color(0x40FFFFFF),
                            Color(0x75FFFFFF),
                            Color(0x40FFFFFF),
                            Color.Transparent
                        ),
                        start = Offset(shineX - shineWidth, coinCenter.y - coinFaceRadius),
                        end = Offset(shineX + shineWidth, coinCenter.y + coinFaceRadius)
                    )
                )
            }

            // 9. Rim Specular Arc Highlight (Top-Left crest reflection)
            drawArc(
                brush = Brush.sweepGradient(
                    0.45f to Color.Transparent,
                    0.55f to Color(0xAAFFFFFF),
                    0.65f to Color(0xEEFFFFFF),
                    0.75f to Color.Transparent,
                    center = coinCenter
                ),
                startAngle = 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(coinCenter.x - baseRadius * 0.94f, coinCenter.y - baseRadius * 0.94f),
                size = androidx.compose.ui.geometry.Size(baseRadius * 1.88f, baseRadius * 1.88f),
                style = Stroke(width = baseRadius * 0.045f, cap = StrokeCap.Round)
            )

            // 10. Accent Sparkling Stars around Coin
            drawSparkleStar(
                center = Offset(coinCenter.x + baseRadius * 0.88f, coinCenter.y - baseRadius * 0.78f),
                size = baseRadius * 0.28f * sparkleScale,
                color = Color(0xFFFFF4B8)
            )
            drawSparkleStar(
                center = Offset(coinCenter.x - baseRadius * 0.85f, coinCenter.y + baseRadius * 0.65f),
                size = baseRadius * 0.20f * (1.8f - sparkleScale),
                color = Color(0xFFFFD54F)
            )
            drawSparkleStar(
                center = Offset(coinCenter.x + baseRadius * 0.98f, coinCenter.y + baseRadius * 0.35f),
                size = baseRadius * 0.15f * sparkleScale,
                color = Color(0xFFFFECB3)
            )
        }
    }
}

/**
 * Draws the sculpted chiseled "F" monogram with bevels and embossed drop shadows.
 */
private fun DrawScope.drawStylizedFMonogram(center: Offset, scale: Float) {
    // Relative unit coordinates scaled to fit coin center
    // The monogram features a vertical pillar, a top horizontal chisel bar,
    // a middle crossbar with a subtle Bengali Taka (৳) angled slash touch.

    val shadowOffset = Offset(scale * 1.6f, scale * 2.2f)

    // Helper function to create the F path
    fun buildFPath(offset: Offset): Path {
        return Path().apply {
            val ox = center.x + offset.x
            val oy = center.y + offset.y

            // Base coordinates for "F" centered around (0,0)
            // Pillar: X from -14 to -4, Y from -22 to +22
            // Top Bar: X from -4 to +16, Y from -22 to -13
            // Middle Bar: X from -4 to +11, Y from -4 to +3
            // Bottom serif: X from -18 to +2, Y from +18 to +22

            moveTo(ox - 18f * scale, oy + 22f * scale) // Base left serif
            lineTo(ox + 2f * scale, oy + 22f * scale)  // Base right serif
            lineTo(ox + 2f * scale, oy + 17f * scale)
            lineTo(ox - 4f * scale, oy + 17f * scale)  // Right edge of pillar bottom
            lineTo(ox - 4f * scale, oy + 3f * scale)   // Below middle bar

            // Middle Bar
            lineTo(ox + 10f * scale, oy + 3f * scale)  // Middle bar bottom
            lineTo(ox + 12f * scale, oy - 2f * scale)  // Chisel right tip
            lineTo(ox + 9f * scale, oy - 4f * scale)   // Middle bar top
            lineTo(ox - 4f * scale, oy - 4f * scale)   // Pillar above middle bar

            // Pillar up to top bar
            lineTo(ox - 4f * scale, oy - 14f * scale)
            lineTo(ox + 14f * scale, oy - 14f * scale) // Top bar bottom
            lineTo(ox + 18f * scale, oy - 18f * scale) // Top bar right chisel point
            lineTo(ox + 16f * scale, oy - 22f * scale) // Top bar top right
            lineTo(ox - 18f * scale, oy - 22f * scale) // Top bar top left serif
            lineTo(ox - 18f * scale, oy - 17f * scale)
            lineTo(ox - 13f * scale, oy - 17f * scale) // Pillar left edge
            lineTo(ox - 13f * scale, oy + 17f * scale)
            lineTo(ox - 18f * scale, oy + 17f * scale)
            close()
        }
    }

    // 1. Embossed Deep Shadow Path
    val shadowPath = buildFPath(shadowOffset)
    drawPath(
        path = shadowPath,
        color = Color(0x993E1D00),
        style = Fill
    )

    // 2. Chiseled Body Path with Metallic Gradient
    val bodyPath = buildFPath(Offset.Zero)
    val bodyGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFDE8), // Top specular highlight
            Color(0xFFFFEE88),
            Color(0xFFFFC000),
            Color(0xFFD67800),
            Color(0xFFA65000)
        ),
        start = Offset(center.x - 16f * scale, center.y - 22f * scale),
        end = Offset(center.x + 16f * scale, center.y + 22f * scale)
    )
    drawPath(
        path = bodyPath,
        brush = bodyGradient,
        style = Fill
    )

    // 3. Highlight Stroke along Top & Left Chisel Edges
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xEEFFFFFF),
                Color(0x88FFEB3B),
                Color(0x33E65100),
                Color(0x11000000)
            ),
            start = Offset(center.x - 20f * scale, center.y - 22f * scale),
            end = Offset(center.x + 20f * scale, center.y + 22f * scale)
        ),
        style = Stroke(width = scale * 1.1f, cap = StrokeCap.Round)
    )

    // 4. Stylized Taka (৳) currency slash accent across middle crossbar
    val slashPath = Path().apply {
        moveTo(center.x - 10f * scale, center.y - 8f * scale)
        lineTo(center.x + 3f * scale, center.y + 7f * scale)
    }
    // Slash shadow
    drawPath(
        path = slashPath,
        color = Color(0x66401E00),
        style = Stroke(width = scale * 2.2f, cap = StrokeCap.Round)
    )
    // Slash golden highlight
    drawPath(
        path = slashPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFDF5), Color(0xFFFFD54F)),
            start = Offset(center.x - 10f * scale, center.y - 8f * scale),
            end = Offset(center.x + 3f * scale, center.y + 7f * scale)
        ),
        style = Stroke(width = scale * 1.4f, cap = StrokeCap.Round)
    )
}

/**
 * Draws a 4-pointed radiant sparkle star at [center] with [size].
 */
private fun DrawScope.drawSparkleStar(
    center: Offset,
    size: Float,
    color: Color
) {
    if (size <= 0f) return

    val starPath = Path().apply {
        moveTo(center.x, center.y - size)
        quadraticTo(center.x, center.y, center.x + size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + size)
        quadraticTo(center.x, center.y, center.x - size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - size)
        close()
    }

    // Outer glow
    drawCircle(
        color = color.copy(alpha = 0.35f),
        center = center,
        radius = size * 0.75f
    )

    // Star body
    drawPath(
        path = starPath,
        color = color,
        style = Fill
    )

    // White core specular dot
    drawCircle(
        color = Color.White,
        center = center,
        radius = size * 0.22f
    )
}
