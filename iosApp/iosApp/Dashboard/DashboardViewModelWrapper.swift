//
// Created by Carlo Huaman Torres on 12/03/26.
//

import Shared
import KMPNativeCoroutinesAsync

struct NutritionSummaryData {
    let totalCalories: Float
    let totalProteinG: Float
    let totalCarbsG: Float
    let totalFatG: Float
    let totalFiberG: Float
    let totalSodiumMg: Float
    let totalSugarG: Float
    let mealCount: Int
    let calorieGoal: Float
    let calorieProgress: Float
    let remainingCalories: Float

    init(from shared: Shared.DailyNutritionSummary) {
        self.totalCalories = shared.totalCalories
        self.totalProteinG = shared.totalProteinG
        self.totalCarbsG = shared.totalCarbsG
        self.totalFatG = shared.totalFatG
        self.totalFiberG = shared.totalFiberG
        self.totalSodiumMg = shared.totalSodiumMg
        self.totalSugarG = shared.totalSugarG
        self.mealCount = Int(shared.mealCount)
        self.calorieGoal = shared.calorieGoal
        self.calorieProgress = shared.calorieProgress
        self.remainingCalories = shared.remainingCalories
    }
}

@MainActor
class DashboardViewModelWrapper: ObservableObject {
    @Published var sleepHours: String = "--"
    @Published var hrv: String = "--"
    @Published var weightKg: String = "--"
    @Published var caloriesBurned: String = "--"
    @Published var alerts: [String] = []
    @Published var foodLogs: [(String, String)] = []
    @Published var todayNutrition: NutritionSummaryData?

    // ViewModel compartido de KMP
    private let dashboardVM: DashboardViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        let userId = ConstantsKt.PROTOTYPE_USER_ID // Dummy
        self.dashboardVM = KoinInitializerKt.getDashboardViewModel(userId: userId)

        observeTask = Task {
            do {
                for try await state in asyncSequence(for: dashboardVM.uiStateFlow) {
                    updateFromState(state)
                }
            } catch {
                print("Error observing state: \(error)")
            }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    func load() {
        dashboardVM.loadDashboard()
    }

    private func updateFromState(_ state: DashboardUiState) {
        if let metrics = state.latestMetrics {
            if let sleep = metrics.sleepHours {
                self.sleepHours = String(format: "%.1f", sleep.floatValue)
            }
            if let hrv = metrics.hrv {
                self.hrv = String(format: "%.0f", hrv.floatValue)
            }
            if let weight = metrics.weightKg {
                self.weightKg = String(format: "%.1f", weight.floatValue)
            }
            if let kcal = metrics.caloriesBurned {
                self.caloriesBurned = "\(kcal.int32Value)"
            }
        }

        self.alerts = state.activeAlerts

        self.foodLogs = state.todayFoodLogs.compactMap { log in
            let mealName = log.mealType.name.capitalized
            return (mealName, log.foodName)
        }

        if let nutrition = state.todayNutrition {
            self.todayNutrition = NutritionSummaryData(from: nutrition)
        } else {
            self.todayNutrition = nil
        }
    }
}
