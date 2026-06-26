package dev.tohure.tanayenai.ui.clinical

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.domain.usecase.ClinicalField
import dev.tohure.tanayenai.presentation.viewmodel.ClinicalProfileViewModel
import dev.tohure.tanayenai.ui.ScreenHeader
import dev.tohure.tanayenai.ui.theme.AccentTerra
import dev.tohure.tanayenai.ui.theme.ErrorRed
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ClinicalProfileScreen(onOpenGeminiToken: () -> Unit = {}) {
    val viewModel: ClinicalProfileViewModel = koinViewModel { parametersOf(PROTOTYPE_USER_ID) }
    val state by viewModel.uiState.collectAsState()
    var selectedField by remember { mutableStateOf<ClinicalField?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = "Perfil Clínico",
                subtitle =
                    "Puedes subir tu PDF de laboratorio, tomar foto del análisis impreso, " +
                        "escribirle a Tanayen tus valores, o ingresarlos aquí manualmente.",
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        // ── PDF ───────────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("📄 Subir análisis (PDF)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Gemini extrae todos los valores automáticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.isExtracting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryGreen,
                                strokeWidth = 2.dp,
                            )
                            Text("Analizando PDF...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = viewModel::pickAndExtractPdf,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen,
                                    contentColor = Color.White,
                                ),
                        ) { Text("Seleccionar PDF") }
                    }
                    if (state.extractionSuccess) {
                        Text(
                            "✓ ${state.extractedValuesCount} valores extraídos",
                            style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryMint),
                        )
                    }
                }
            }
        }

        // ── Foto o chat — aviso informativo ───────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5EE)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "📷 Foto o 💬 Chat",
                        style = MaterialTheme.typography.titleMedium.copy(color = PrimaryGreen),
                    )
                    Text(
                        "También puedes tomar una foto de tu análisis impreso desde el chat, " +
                            "o simplemente escribirle a Tanayen: \"mi glucosa hoy fue 98\". " +
                            "Te preguntará si quieres guardarlo.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryGreen),
                    )
                }
            }
        }

        // ── Formulario manual ─────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("✏️ Agregar valor individual", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Para valores como glucosa de un glucómetro o presión arterial.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ClinicalField.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { field ->
                                OutlinedButton(
                                    onClick = { selectedField = field },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        field.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ── Restricciones activas ─────────────────────────────────────────────
        state.profile?.let { profile ->
            if (profile.activeRestrictions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Restricciones activas",
                                style = MaterialTheme.typography.titleMedium.copy(color = AccentTerra),
                            )
                            profile.activeRestrictions.forEach { restriction ->
                                Text(
                                    "• $restriction",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = AccentTerra),
                                )
                            }
                        }
                    }
                }
            }

            // Grupos de valores — solo se agrega el item si tiene al menos un valor
            val groups =
                listOf(
                    "Perfil lipídico" to lipidValues(profile),
                    "Glucosa" to glucoseValues(profile),
                    "Función renal" to renalValues(profile),
                    "Función hepática" to hepaticValues(profile),
                    "Tiroides" to thyroidValues(profile),
                    "Hemograma" to hemogramValues(profile),
                    "Vitaminas y minerales" to vitaminValues(profile),
                    "Inflamación" to inflammationValues(profile),
                )
            groups.forEach { (title, values) ->
                if (values.any { it.second != null }) {
                    item { ClinicalGroupCard(title, values) }
                }
            }
        }

        state.error?.let { error ->
            item {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium.copy(color = ErrorRed),
                )
            }
        }

        // ── Easter egg ────────────────────────────────────────────────────────
        item {
            EasterEggLeaf(onTriggered = onOpenGeminiToken)
        }
    }

    selectedField?.let { field ->
        ManualValueDialog(
            field = field,
            onConfirm = { value ->
                viewModel.saveIndividualValue(field, value)
                selectedField = null
            },
            onDismiss = { selectedField = null },
        )
    }
}

@Composable
private fun ClinicalGroupCard(
    title: String,
    values: List<Pair<String, String?>>,
) {
    val nonNull = values.filter { it.second != null }
    if (nonNull.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            nonNull.forEach { (name, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualValueDialog(
    field: ClinicalField,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.displayName) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Valor en ${field.unit}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { text.toFloatOrNull()?.let { onConfirm(it) } },
                enabled = text.toFloatOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun lipidValues(p: ClinicalProfile) =
    listOf(
        "Colesterol total" to p.cholesterolTotal?.let { "${it.toInt()} mg/dL" },
        "HDL" to p.hdl?.let { "${it.toInt()} mg/dL" },
        "LDL" to p.ldl?.let { "${it.toInt()} mg/dL" },
        "Triglicéridos" to p.triglycerides?.let { "${it.toInt()} mg/dL" },
    )

private fun glucoseValues(p: ClinicalProfile) =
    listOf(
        "Glucosa en ayunas" to p.fastingGlucose?.let { "${it.toInt()} mg/dL" },
        "HbA1c" to p.hba1c?.let { "$it %" },
        "Insulina en ayunas" to p.fastingInsulin?.let { "$it µU/mL" },
        "HOMA-IR" to p.homaIr?.let { "$it" },
    )

private fun renalValues(p: ClinicalProfile) =
    listOf(
        "Creatinina" to p.creatinine?.let { "$it mg/dL" },
        "Urea" to p.urea?.let { "${it.toInt()} mg/dL" },
        "FG estimado" to p.gfr?.let { "${it.toInt()} mL/min" },
        "Ácido úrico" to p.uricAcid?.let { "$it mg/dL" },
    )

private fun hepaticValues(p: ClinicalProfile) =
    listOf(
        "ALT/TGP" to p.alt?.let { "${it.toInt()} U/L" },
        "AST/TGO" to p.ast?.let { "${it.toInt()} U/L" },
        "GGT" to p.ggt?.let { "${it.toInt()} U/L" },
    )

private fun thyroidValues(p: ClinicalProfile) =
    listOf(
        "TSH" to p.tsh?.let { "$it µU/mL" },
        "T3 libre" to p.t3Free?.let { "$it pg/mL" },
        "T4 libre" to p.t4Free?.let { "$it ng/dL" },
    )

private fun hemogramValues(p: ClinicalProfile) =
    listOf(
        "Hemoglobina" to p.hemoglobin?.let { "$it g/dL" },
        "Hematocrito" to p.hematocrit?.let { "$it %" },
        "Ferritina" to p.ferritin?.let { "${it.toInt()} ng/mL" },
        "Hierro sérico" to p.serumIron?.let { "${it.toInt()} µg/dL" },
    )

private fun vitaminValues(p: ClinicalProfile) =
    listOf(
        "Vitamina D" to p.vitaminD?.let { "${it.toInt()} ng/mL" },
        "Vitamina B12" to p.vitaminB12?.let { "${it.toInt()} pg/mL" },
        "Folato" to p.folate?.let { "$it ng/mL" },
        "Zinc" to p.zinc?.let { "${it.toInt()} µg/dL" },
        "Magnesio" to p.magnesium?.let { "$it mg/dL" },
    )

private fun inflammationValues(p: ClinicalProfile) =
    listOf(
        "PCR ultrasensible" to p.crpUltraSensitive?.let { "$it mg/L" },
        "Homocisteína" to p.homocysteine?.let { "$it µmol/L" },
    )

@Composable
private fun EasterEggLeaf(onTriggered: () -> Unit) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🌿",
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier.clickable {
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapTime < 600) tapCount + 1 else 1
                    lastTapTime = now
                    if (tapCount >= 5) {
                        tapCount = 0
                        onTriggered()
                    }
                },
        )
    }
}
