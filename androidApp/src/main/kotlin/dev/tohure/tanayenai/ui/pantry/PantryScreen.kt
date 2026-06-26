package dev.tohure.tanayenai.ui.pantry

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.tanayenai.R
import dev.tohure.tanayenai.domain.model.IngredientCategory
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.presentation.model.CategorizedItem
import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import dev.tohure.tanayenai.ui.ScreenHeader
import dev.tohure.tanayenai.ui.theme.BackgroundColor
import dev.tohure.tanayenai.ui.theme.ErrorRed
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen() {
    val viewModel: PantryViewModel = koinViewModel { parametersOf(PROTOTYPE_USER_ID) }
    val state by viewModel.uiState.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundColor,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = PrimaryGreen,
                contentColor = SurfaceColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_add_item), contentDescription = "Agregar ingrediente")
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Header
            ScreenHeader(
                title = "Alacena",
                subtitle = "${state.categoryGroups.sumOf { it.items.size }} ingredientes",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            // Search bar
            BasicTextField(
                value = state.searchQuery,
                onValueChange = viewModel::search,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 12.dp)
                        .onFocusChanged { searchFocused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = if (searchFocused) SecondaryMint else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(24.dp),
                        ).clip(RoundedCornerShape(24.dp))
                        .background(SurfaceColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextDark),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            tint = TextMutedColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Box {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    "Buscar ingrediente...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMutedColor,
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }

                state.categoryGroups.isEmpty() -> {
                    EmptyPantryState(onAdd = { showAddSheet = true })
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.categoryGroups.forEach { group ->
                            item(key = group.category.name) {
                                CategorySection(
                                    category = group.category,
                                    items = group.items,
                                    onEdit = { viewModel.openEdit(it) },
                                    onDelete = { viewModel.deleteItem(it.id) },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) } // room for FAB
                    }
                }
            }
        }
    }

    // Edit bottom sheet
    state.editingItem?.let { item ->
        EditItemSheet(
            item = item,
            isSaving = state.isSaving,
            onSave = { qty, unit, expiry -> viewModel.saveEdit(item.id, qty, unit, expiry) },
            onDismiss = viewModel::closeEdit,
        )
    }

    // Add bottom sheet
    if (showAddSheet) {
        AddItemSheet(
            isSaving = state.isSaving,
            onAdd = { ingredient, qty, unit, expiry ->
                viewModel.addItem(ingredient, qty, unit, expiry)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }

    // Error handling
    state.error?.let {
        LaunchedEffect(it) { viewModel.clearError() }
    }
}

// ── Category section ──────────────────────────────────────────────────────────

@Composable
private fun CategorySection(
    category: IngredientCategory,
    items: List<CategorizedItem>,
    onEdit: (PantryItem) -> Unit,
    onDelete: (PantryItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(category.emoji, fontSize = 18.sp)
                Text(category.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "(${items.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedColor,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) "Ocultar" else "Ver",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGreen,
                )
            }
        }

        if (expanded) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    items.forEachIndexed { index, categorized ->
                        IngredientRow(
                            item = categorized.item,
                            onEdit = { onEdit(categorized.item) },
                            onDelete = { onDelete(categorized.item) },
                        )
                        if (index < items.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFF0F0F0),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Ingredient row ────────────────────────────────────────────────────────────

@Composable
private fun IngredientRow(
    item: PantryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.ingredient.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${item.quantity} ${item.unit.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedColor,
                )
                item.expiryDate?.let { expiry ->
                    Text(
                        "· Vence: $expiry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedColor,
                    )
                }
            }
        }

        Row {
            IconButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Editar",
                    tint = TextMutedColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove_item),
                    contentDescription = "Eliminar",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar ingrediente") },
            text = { Text("¿Eliminar ${item.ingredient} de tu alacena?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyPantryState(onAdd: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🥬", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Tu alacena está vacía", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Toma una foto de tu alacena desde el chat o agrega ingredientes manualmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedColor,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(12.dp),
        ) { Text("+ Agregar ingrediente") }
    }
}

// ── Edit bottom sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemSheet(
    item: PantryItem,
    isSaving: Boolean,
    onSave: (Float, PantryUnit, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var expiryDate by remember { mutableStateOf(item.expiryDate ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                item.ingredient.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            Text("Unidad", style = MaterialTheme.typography.bodyMedium)
            UnitSelector(selected = unit, onSelect = { unit = it })

            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text("Fecha de vencimiento (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text("Opcional") },
            )

            Button(
                onClick = {
                    val qty = quantity.toFloatOrNull() ?: return@Button
                    onSave(qty, unit, expiryDate.takeIf { it.isNotBlank() })
                },
                enabled = quantity.toFloatOrNull() != null && !isSaving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SurfaceColor, strokeWidth = 2.dp)
                } else {
                    Text("Guardar cambios")
                }
            }
        }
    }
}

// ── Add bottom sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(
    isSaving: Boolean,
    onAdd: (String, Float, PantryUnit, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var ingredient by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(PantryUnit.UNITS) }
    var expiryDate by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Agregar ingrediente", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = ingredient,
                onValueChange = { ingredient = it },
                label = { Text("Nombre del ingrediente") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text("ej: avena, almendras, leche...") },
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            Text("Unidad", style = MaterialTheme.typography.bodyMedium)
            UnitSelector(selected = unit, onSelect = { unit = it })

            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text("Fecha de vencimiento (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text("Opcional") },
            )

            Button(
                onClick = {
                    val qty = quantity.toFloatOrNull() ?: return@Button
                    if (ingredient.isBlank()) return@Button
                    onAdd(ingredient, qty, unit, expiryDate.takeIf { it.isNotBlank() })
                },
                enabled = ingredient.isNotBlank() && quantity.toFloatOrNull() != null && !isSaving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SurfaceColor, strokeWidth = 2.dp)
                } else {
                    Text("Agregar")
                }
            }
        }
    }
}

// ── Unit selector chips ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSelector(
    selected: PantryUnit,
    onSelect: (PantryUnit) -> Unit,
) {
    val commonUnits =
        listOf(PantryUnit.UNITS, PantryUnit.GRAMS, PantryUnit.KG, PantryUnit.ML, PantryUnit.L, PantryUnit.CUPS)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(commonUnits) { unit ->
            FilterChip(
                selected = unit == selected,
                onClick = { onSelect(unit) },
                label = { Text(unit.name.lowercase(), style = MaterialTheme.typography.labelSmall) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen,
                        selectedLabelColor = SurfaceColor,
                    ),
            )
        }
    }
}
