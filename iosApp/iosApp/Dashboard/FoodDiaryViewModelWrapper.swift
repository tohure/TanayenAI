//
//  FoodDiaryViewModelWrapper.swift
//

import Shared
import KMPNativeCoroutinesAsync

struct FoodDiaryEntryData: Identifiable, Equatable {
    let id: String
    let time: String
    let mealType: String
    let foodName: String
    let calories: Int
}

struct FoodDiaryDayData: Identifiable, Equatable {
    let id: String          // fecha (yyyy-MM-dd)
    let label: String
    let totalCalories: Int
    let mealCount: Int
    let entries: [FoodDiaryEntryData]
}

@MainActor
final class FoodDiaryViewModelWrapper: ObservableObject {

    @Published var days: [FoodDiaryDayData] = []
    @Published var isLoading: Bool = true
    @Published var isEmpty: Bool = false

    private let vm: FoodDiaryViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        vm = KoinInitializerKt.getFoodDiaryViewModel(userId: ConstantsKt.PROTOTYPE_USER_ID)

        // Snapshot inicial para primer render sin parpadeo.
        apply(vm.uiState)

        observeTask = Task {
            do {
                for try await state in asyncSequence(for: vm.uiStateFlow) {
                    self.apply(state)
                }
            } catch { }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    func deleteEntry(_ id: String) {
        vm.deleteEntry(id: id)
    }

    private func apply(_ state: FoodDiaryUiState) {
        self.isLoading = state.isLoading
        self.isEmpty = state.isEmpty
        self.days = state.days.map { day in
            FoodDiaryDayData(
                id: day.date,
                label: day.label,
                totalCalories: Int(day.totalCalories),
                mealCount: Int(day.mealCount),
                entries: day.entries.map { entry in
                    FoodDiaryEntryData(
                        id: entry.id,
                        time: UtilsKt.timeFromIso(iso: entry.loggedAt),
                        mealType: entry.mealType.displayName,
                        foodName: entry.foodName,
                        calories: Int(entry.calories)
                    )
                }
            )
        }
    }
}
