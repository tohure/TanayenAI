package dev.tohure.tanayenai.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.domain.model.timeFromIso
import dev.tohure.tanayenai.presentation.viewmodel.FoodDiaryDay
import dev.tohure.tanayenai.presentation.viewmodel.FoodDiaryViewModel
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FoodDiaryScreen(onBack: () -> Unit = {}) {
    val viewModel: FoodDiaryViewModel = koinViewModel { parametersOf(PROTOTYPE_USER_ID) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<FoodLog?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        DiaryHeader(onBack = onBack)

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            }

            uiState.isEmpty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay comidas registradas 🍽️",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedColor,
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    uiState.days.forEach { day ->
                        item(key = day.date) { DaySectionHeader(day) }
                        items(day.entries, key = { it.id }) { entry ->
                            SwipeableEntryRow(
                                entry = entry,
                                onRequestDelete = { pendingDelete = entry },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Borrar este registro?") },
            text = {
                Text("Se quitará \"${entry.foodName}\" y se recalcularán las calorías del día.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Borrar", color = Color(0xFFE63946))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = TextMutedColor)
                }
            },
        )
    }
}

@Composable
private fun DiaryHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("←", style = MaterialTheme.typography.headlineSmall, color = TextDark)
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Diario de comidas",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun DaySectionHeader(day: FoodDiaryDay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${day.totalCalories.toInt()} kcal · ${day.mealCount} comida${if (day.mealCount != 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedColor,
        )
    }
}

@Composable
private fun SwipeableEntryRow(
    entry: FoodLog,
    onRequestDelete: () -> Unit,
) {
    // Al deslizar, solo pide confirmación (retorna false para que la fila no se descarte);
    // el borrado real ocurre tras aceptar el diálogo.
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onRequestDelete()
                }
                false
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE63946))
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Borrar", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        },
    ) {
        EntryRow(entry)
    }
}

@Composable
private fun EntryRow(entry: FoodLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.size(width = 64.dp, height = 36.dp)) {
                Text(
                    text = timeFromIso(entry.loggedAt),
                    style = MaterialTheme.typography.labelMedium.copy(color = TextDark),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.mealType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedColor,
                )
            }
            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextDark),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${entry.calories.toInt()} kcal",
                style = MaterialTheme.typography.labelMedium.copy(color = PrimaryGreen),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }
    }
}
