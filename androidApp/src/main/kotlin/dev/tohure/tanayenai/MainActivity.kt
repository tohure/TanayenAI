package dev.tohure.tanayenai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dev.tohure.tanayenai.data.pdf.PdfPicker
import dev.tohure.tanayenai.data.remote.SyncManager
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.ui.navigation.TanayenNavigation
import dev.tohure.tanayenai.ui.theme.TanayenTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )

        // PdfPicker must be instantiated eagerly here so registerForActivityResult
        // is called while the Activity is still in CREATED state (before onStart).
        val pdfPicker = PdfPicker(activity = this@MainActivity)
        loadKoinModules(
            module {
                single { pdfPicker }
            },
        )

        // Pull de datos remotos en el Scope de la Activity
        lifecycleScope.launch {
            Log.d("TANAYEN_DEBUG", "=== MAIN ACTIVITY: Starting pullRemoteData ===")
            try {
                get<SyncManager>().pullRemoteData(PROTOTYPE_USER_ID)
            } catch (e: Exception) {
                Log.e("TANAYEN_DEBUG", "pullRemoteData ERROR: ${e.message}", e)
            }
        }

        setContent {
            TanayenTheme {
                TanayenNavigation()
            }
        }
    }
}
