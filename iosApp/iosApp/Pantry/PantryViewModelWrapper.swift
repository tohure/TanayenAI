import KMPNativeCoroutinesAsync
import Shared
import SwiftUI

/// Bridges the KMP PantryViewModel (StateFlow) to SwiftUI's ObservableObject.
/// Follows the same pattern as ChatViewModelWrapper and DashboardViewModelWrapper.
@MainActor
class PantryViewModelWrapper: ObservableObject {
    /// Pre-sorted category groups — iterates a flat [CategoryGroup] array, no Dictionary casts.
    @Published var categoryGroups: [CategoryGroup] = []
    @Published var isLoading: Bool = true
    @Published var isSaving: Bool = false
    @Published var editingItem: PantryItem_?

    private let pantryVM: PantryViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        let userId = ConstantsKt.PROTOTYPE_USER_ID
        self.pantryVM = KoinInitializerKt.getPantryViewModel(userId: userId)

        observeTask = Task {
            do {
                for try await state in asyncSequence(for: pantryVM.uiStateFlow) {
                    self.isLoading = state.isLoading
                    self.isSaving = state.isSaving
                    self.editingItem = state.editingItem
                    // categoryGroups is List<CategoryGroup> — no casting needed
                    self.categoryGroups = state.categoryGroups as? [CategoryGroup] ?? []
                }
            } catch {
                print("PantryViewModelWrapper: state observe error \(error)")
            }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    // MARK: - Actions

    /// Recarga desde la BD. Se llama en onAppear para reflejar ingredientes agregados
    /// desde el chat (otro ViewModel) sin reiniciar la app.
    func reload() {
        pantryVM.loadItems()
    }

    func search(_ query: String) {
        pantryVM.search(query: query)
    }

    func startEdit(_ item: PantryItem_) {
        pantryVM.openEdit(item: item)
    }

    func closeEdit() {
        pantryVM.closeEdit()
    }

    func saveEdit(itemId: String, quantity: Float, unit: PantryUnit, expiryDate: String?) {
        pantryVM.saveEdit(itemId: itemId, newQuantity: quantity, newUnit: unit, newExpiryDate: expiryDate)
    }

    func addItem(ingredient: String, quantity: Float, unit: PantryUnit, expiryDate: String?) {
        pantryVM.addItem(
            ingredient: ingredient,
            quantity: quantity,
            unit: unit,
            expiryDate: expiryDate,
            locationId: ConstantsKt.DEFAULT_LOCATION_ID
        )
    }

    func deleteItem(_ item: PantryItem_) {
        pantryVM.deleteItem(itemId: item.id)
    }
}
