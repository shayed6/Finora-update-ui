package com.example.ui.screens.pdf

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityOptionsCompat
import com.example.ui.theme.PrimaryBlue

private class FallbackRegistryOwner : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            // Safe fallback no-op if no activity registry is attached
        }
    }
}

private fun findRegistryOwner(context: Context): ActivityResultRegistryOwner? {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is ActivityResultRegistryOwner) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun PdfPreviewModal(
    preview: ActivePdfPreview,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSaveToUri: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
        ?: findRegistryOwner(context)
        ?: remember { FallbackRegistryOwner() }

    CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
        PdfPreviewModalContent(
            preview = preview,
            onClose = onClose,
            onShare = onShare,
            onPrint = onPrint,
            onSaveToUri = onSaveToUri
        )
    }
}

@Composable
private fun PdfPreviewModalContent(
    preview: ActivePdfPreview,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSaveToUri: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    val totalPages = preview.pageBitmaps.size

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onSaveToUri(uri)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("modal_pdf_preview"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar for Preview
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("btn_close_preview")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close preview")
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = preview.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = preview.file.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Share Action
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.testTag("btn_share_pdf")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = PrimaryBlue)
                        }

                        // Print Action
                        IconButton(
                            onClick = onPrint,
                            modifier = Modifier.testTag("btn_print_pdf")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print PDF")
                        }

                        // Save to Device via SAF
                        IconButton(
                            onClick = {
                                try {
                                    createDocLauncher.launch(preview.file.name)
                                } catch (_: Exception) {
                                    onShare()
                                }
                            },
                            modifier = Modifier.testTag("btn_save_pdf")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save to device")
                        }
                    }
                }

                // Page Indicator Bar
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "পৃষ্ঠা ${currentPageIndex + 1} / $totalPages",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            enabled = currentPageIndex < totalPages - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }

                // Content Viewer: High-res interactive page
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF525659))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val currentBitmap = preview.pageBitmaps.getOrNull(currentPageIndex)
                    if (currentBitmap != null) {
                        var scale by remember(currentPageIndex) { mutableFloatStateOf(1f) }
                        var offset by remember(currentPageIndex) { mutableStateOf(Offset.Zero) }

                        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                            scale = (scale * zoomChange).coerceIn(0.8f, 3.5f)
                            offset += offsetChange
                        }

                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(state = transformState)
                        ) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "PDF Page ${currentPageIndex + 1}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Text(
                            text = "পৃষ্ঠা লোড করা যায়নি",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
