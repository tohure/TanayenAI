//
// Created by Carlo Huaman Torres on 12/03/26.
//

import Shared
import KMPNativeCoroutinesAsync

@MainActor
class DashboardViewModelWrapper: ObservableObject {
    @Published var sleepHours: String = "--"
    @Published var hrv: String = "--"
    @Published var weightKg: String = "--"
    @Published var caloriesBurned: String = "--"
    @Published var alerts: [String] = []
    @Published var foodLogs: [(String, String)] = []

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
            if let weightKg = metrics.weightKg {
                self.weightKg = String(format: "%.1f", weightKg.floatValue)
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
    }
}
