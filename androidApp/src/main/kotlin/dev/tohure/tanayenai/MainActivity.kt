package dev.tohure.tanayenai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.tohure.tanayenai.ui.navigation.TanayenNavigation
import dev.tohure.tanayenai.ui.theme.TanayenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TanayenTheme {
                TanayenNavigation()
            }
        }
    }
}
