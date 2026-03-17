package dev.tohure.tanayenai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.lifecycleScope
import dev.tohure.tanayenai.data.remote.SyncManager
import dev.tohure.tanayenai.ui.navigation.TanayenNavigation
import dev.tohure.tanayenai.ui.theme.TanayenTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    // Launcher de permisos Health Connect
    private val permissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted ->
            Log.d("TANAYEN_DEBUG", "Health Connect permissions granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pull de datos remotos en el Scope de la Activity
        lifecycleScope.launch {
            Log.d("TANAYEN_DEBUG", "=== MAIN ACTIVITY: Starting pullRemoteData ===")
            try {
                get<SyncManager>().pullRemoteData("00000000-0000-0000-0000-000000000001")
            } catch (e: Exception) {
                Log.e("TANAYEN_DEBUG", "pullRemoteData ERROR: ${e.message}", e)
            }
        }

        requestHealthPermissionsIfNeeded()

        setContent {
            TanayenTheme {
                TanayenNavigation()
            }
        }
    }

    private fun requestHealthPermissionsIfNeeded() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) return

        val permissions =
            setOf(
                HealthPermission.getReadPermission(SleepSessionRecord::class),
                HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
                HealthPermission.getReadPermission(RestingHeartRateRecord::class),
                HealthPermission.getReadPermission(WeightRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
            )
        permissionLauncher.launch(permissions)
    }
}
