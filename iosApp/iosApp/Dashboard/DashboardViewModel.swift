import Foundation
import Shared

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var sleepHours: String = "--"
    @Published var hrv: String = "--"
    @Published var weightKg: String = "--"
    @Published var restingHeartRate: String = "--"
    @Published var alerts: [String] = []
    @Published var foodLogs: [(String, String)] = []

    // ViewModel compartido de KMP
    private let kmpViewModel: Shared.DashboardViewModel

    init() {
        let userId = "00000000-0000-0000-0000-000000000001" // Dummy
        self.kmpViewModel = KoinInitializerKt.getDashboardViewModel(userId: userId)

        // Escuchando cambios en el estado
        kmpViewModel.observeUiStateIos { [weak self] state in
            Task { @MainActor in
                self?.updateFromState(state)
            }
        }
    }

    func load() {
        kmpViewModel.loadDashboard()
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
            if let bpm = metrics.restingHeartRate {
                self.restingHeartRate = "\(bpm.int32Value)"
            }
        }
        self.alerts = state.activeAlerts

        self.foodLogs = state.todayFoodLogs.compactMap { log in
            let mealName = log.mealType.name.capitalized
            return (mealName, log.foodName)
        }
    }
}
