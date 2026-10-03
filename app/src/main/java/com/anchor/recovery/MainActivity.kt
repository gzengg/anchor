package com.anchor.recovery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anchor.recovery.ui.AnchorApp
import com.anchor.recovery.ui.theme.AnchorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35（Android 15）强制 edge-to-edge：显式开启，避免不同 OEM 行为差异。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val anchorApplication = application as AnchorApplication
        setContent {
            AnchorTheme {
                AnchorApp(
                    repository = anchorApplication.repository,
                    contentRepository = anchorApplication.contentRepository,
                    settings = anchorApplication.settings,
                )
            }
        }
    }
}
