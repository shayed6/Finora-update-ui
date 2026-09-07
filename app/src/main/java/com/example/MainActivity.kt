package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.FinoraApp
import com.example.ui.screens.FinoraSplashScreen
import com.example.ui.theme.FinoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinoraTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 400),
                    label = "FinoraSplashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        FinoraSplashScreen(onFinish = { showSplash = false })
                    } else {
                        FinoraApp(onShowSplash = { showSplash = true })
                    }
                }
            }
        }
    }
}


