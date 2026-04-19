import Shared
import SwiftUI

struct PantryView: View {
    @StateObject private var viewModel = PantryViewModelWrapper()
    @State private var showAddSheet = false
    @State private var searchText = ""

    var body: some View {
        VStack(spacing: 0) {
            // ── Header ────────────────────────────────────────────────────────
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Alacena")
                        .font(.system(size: 22, weight: .semibold, design: .rounded))
                        .foregroundColor(TanayenTheme.textDark)
                    let totalItems = viewModel.categoryGroups.reduce(0) { $0 + $1.items.count }
                    Text("\(totalItems) ingredientes")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }
                Spacer()
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(TanayenTheme.primaryGreen)
                }
                .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)

            // ── Search bar ────────────────────────────────────────────────────
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(TanayenTheme.textMuted)
                    .font(.system(size: 15))
                TextField("", text: $searchText)
                    .font(.system(.body, design: .rounded))
                    .foregroundColor(TanayenTheme.textDark)
                    .overlay(alignment: .leading) {
                        if searchText.isEmpty {
                            Text("Buscar ingrediente...")
                                .font(.system(.body, design: .rounded))
                                .foregroundColor(TanayenTheme.textMuted)
                                .allowsHitTesting(false)
                        }
                    }
                    .onChange(of: searchText) { viewModel.search($0) }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(TanayenTheme.surface)
            .cornerRadius(24)
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color(hex: "#E0E0E0"), lineWidth: 1))
            .padding(.horizontal, 24)
            .padding(.bottom, 12)

            // ── Content ───────────────────────────────────────────────────────
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(TanayenTheme.background)
            } else if viewModel.categoryGroups.isEmpty {
                EmptyPantryView(onAdd: { showAddSheet = true })
            } else {
                List {
                    ForEach(viewModel.categoryGroups, id: \.category.name) { group in
                        PantryCategorySection(
                            category: group.category,
                            items: group.items as? [CategorizedItem] ?? [],
                            onEdit: { viewModel.startEdit($0) },
                            onDelete: { viewModel.deleteItem($0) }
                        )
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
                .background(TanayenTheme.background)
            }
        }
        .background(TanayenTheme.background)
        .sheet(isPresented: $showAddSheet) {
            AddPantryItemSheet(
                isSaving: viewModel.isSaving,
                onAdd: { ingredient, qty, unit, expiry in
                    viewModel.addItem(ingredient: ingredient, quantity: qty, unit: unit, expiryDate: expiry)
                    showAddSheet = false
                },
                onDismiss: { showAddSheet = false }
            )
        }
        .sheet(
            item: $viewModel.editingItem,
            content: { item in
                EditPantryItemSheet(
                    item: item,
                    isSaving: viewModel.isSaving,
                    onSave: { qty, unit, expiry in
                        viewModel.saveEdit(itemId: item.id, quantity: qty, unit: unit, expiryDate: expiry)
                    },
                    onDismiss: { viewModel.closeEdit() }
                )
            }
        )
    }
}

// ── Category section ──────────────────────────────────────────────────────────

struct PantryCategorySection: View {
    let category: IngredientCategory
    let items: [CategorizedItem]
    let onEdit: (PantryItem_) -> Void
    let onDelete: (PantryItem_) -> Void

    var body: some View {
        Section(header:
            HStack(spacing: 8) {
                Text(category.emoji)
                Text(category.displayName)
                    .font(.system(.subheadline, design: .rounded, weight: .semibold))
                    .foregroundColor(TanayenTheme.textDark)
                Text("(\(items.count))")
                    .font(.system(.caption, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
            }
            .listRowInsets(EdgeInsets())
            .padding(.horizontal, 4)
        ) {
            ForEach(items, id: \.item.id) { categorized in
                PantryIngredientRow(
                    item: categorized.item,
                    onEdit: { onEdit(categorized.item) }
                )
                .listRowBackground(TanayenTheme.surface)
                .listRowSeparatorTint(Color(hex: "#F0F0F0"))
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        onDelete(categorized.item)
                    } label: {
                        Label("Eliminar", systemImage: "trash")
                    }
                    Button { onEdit(categorized.item) } label: {
                        Label("Editar", systemImage: "pencil")
                    }
                    .tint(TanayenTheme.primaryGreen)
                }
            }
        }
    }
}

// ── Ingredient row ────────────────────────────────────────────────────────────

struct PantryIngredientRow: View {
    let item: PantryItem_
    let onEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(item.ingredient.prefix(1).uppercased() + item.ingredient.dropFirst())
                .font(.system(.body, design: .rounded, weight: .semibold))
                .foregroundColor(TanayenTheme.textDark)
            HStack(spacing: 6) {
                Text("\(item.quantity.formatted()) \(item.unit.name.lowercased())")
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
                if let expiry = item.expiryDate {
                    Text("· Vence: \(expiry)")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture { onEdit() }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

struct EmptyPantryView: View {
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("🥬").font(.system(size: 48))
            Text("Tu alacena está vacía")
                .font(.system(.headline, design: .rounded))
                .foregroundColor(TanayenTheme.textDark)
            Text("Toma una foto desde el chat o agrega ingredientes manualmente.")
                .font(.system(.subheadline, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Button(action: onAdd) {
                Text("+ Agregar ingrediente")
                    .font(.system(.headline, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(TanayenTheme.primaryGreen)
                    .cornerRadius(12)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(TanayenTheme.background)
    }
}

// ── Shared form content ───────────────────────────────────────────────────────

private struct PantryItemFormContent: View {
    let title: String
    @Binding var ingredient: String
    @Binding var quantity: String
    @Binding var unit: PantryUnit
    @Binding var expiryDate: String
    let isIngredientEditable: Bool
    let isSaving: Bool
    let buttonLabel: String
    let onSubmit: () -> Void
    let onDismiss: () -> Void

    private let commonUnits: [PantryUnit] = [.units, .grams, .kg, .ml, .l, .cups]

    var body: some View {
        NavigationView {
            Form {
                if isIngredientEditable {
                    Section("Ingrediente") {
                        TextField("ej: avena, almendras...", text: $ingredient)
                            .font(.system(.body, design: .rounded))
                    }
                }
                Section("Cantidad") {
                    TextField("Cantidad", text: $quantity)
                        .keyboardType(.decimalPad)
                        .font(.system(.body, design: .rounded))
                }
                Section("Unidad") {
                    Picker("Unidad", selection: $unit) {
                        ForEach(commonUnits, id: \.self) { pantryUnit in
                            Text(pantryUnit.name.lowercased()).tag(pantryUnit)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Fecha de vencimiento (opcional)") {
                    TextField("YYYY-MM-DD", text: $expiryDate)
                        .font(.system(.body, design: .rounded))
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar", action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(buttonLabel, action: onSubmit)
                            .foregroundColor(TanayenTheme.primaryGreen)
                            .disabled(quantity.isEmpty || (isIngredientEditable && ingredient.isEmpty))
                    }
                }
            }
        }
    }
}

// ── Edit sheet ────────────────────────────────────────────────────────────────

struct EditPantryItemSheet: View {
    let item: PantryItem_
    let isSaving: Bool
    let onSave: (Float, PantryUnit, String?) -> Void
    let onDismiss: () -> Void

    @State private var quantity: String
    @State private var unit: PantryUnit
    @State private var expiryDate: String

    init(item: PantryItem_, isSaving: Bool,
         onSave: @escaping (Float, PantryUnit, String?) -> Void,
         onDismiss: @escaping () -> Void) {
        self.item = item
        self.isSaving = isSaving
        self.onSave = onSave
        self.onDismiss = onDismiss
        _quantity = State(initialValue: String(item.quantity))
        _unit = State(initialValue: item.unit)
        _expiryDate = State(initialValue: item.expiryDate ?? "")
    }

    var body: some View {
        PantryItemFormContent(
            title: item.ingredient.prefix(1).uppercased() + item.ingredient.dropFirst(),
            ingredient: .constant(item.ingredient),
            quantity: $quantity,
            unit: $unit,
            expiryDate: $expiryDate,
            isIngredientEditable: false,
            isSaving: isSaving,
            buttonLabel: "Guardar cambios",
            onSubmit: {
                guard let qty = Float(quantity) else { return }
                onSave(qty, unit, expiryDate.isEmpty ? nil : expiryDate)
            },
            onDismiss: onDismiss
        )
    }
}

// ── Add sheet ─────────────────────────────────────────────────────────────────

struct AddPantryItemSheet: View {
    let isSaving: Bool
    let onAdd: (String, Float, PantryUnit, String?) -> Void
    let onDismiss: () -> Void

    @State private var ingredient = ""
    @State private var quantity = ""
    @State private var unit = PantryUnit.units
    @State private var expiryDate = ""

    var body: some View {
        PantryItemFormContent(
            title: "Agregar ingrediente",
            ingredient: $ingredient,
            quantity: $quantity,
            unit: $unit,
            expiryDate: $expiryDate,
            isIngredientEditable: true,
            isSaving: isSaving,
            buttonLabel: "Agregar",
            onSubmit: {
                guard let qty = Float(quantity), !ingredient.isEmpty else { return }
                onAdd(ingredient, qty, unit, expiryDate.isEmpty ? nil : expiryDate)
            },
            onDismiss: onDismiss
        )
    }
}

// MARK: - Protocol conformances for KMP bridged types

/// Required for `.sheet(item: $vm.editingItem)` to work with the KMP-bridged PantryItem.
extension PantryItem_: Identifiable {}
