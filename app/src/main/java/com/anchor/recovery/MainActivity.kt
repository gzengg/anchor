package com.anchor.recovery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.anchor.recovery.ui.theme.AnchorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35（Android 15）强制 edge-to-edge：显式开启，避免不同 OEM 行为差异。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AnchorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    AnchorRoot(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnchorRoot(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = AppInfo.DISPLAY_NAME, style = MaterialTheme.typography.headlineLarge)
    }
}
