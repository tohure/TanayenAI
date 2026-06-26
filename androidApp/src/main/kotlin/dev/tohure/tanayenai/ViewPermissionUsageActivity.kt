package dev.tohure.tanayenai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.ui.theme.TanayenTheme

class ViewPermissionUsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TanayenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Privacidad y Salud",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text =
                                "TanayenAI utiliza tus datos de Health Connect (sueño, variabilidad " +
                                    "cardíaca, calorías, peso y pasos) para brindarte una nutrición " +
                                    "inteligente y personalizada basada en tu actividad real diaria.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
