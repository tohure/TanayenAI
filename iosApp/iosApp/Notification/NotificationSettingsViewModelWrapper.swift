import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class NotificationSettingsViewModelWrapper: ObservableObject {

    @Published var morningEnabled: Bool = false
    @Published var hour: Int = 7
    @Published var minute: Int = 0

    private let viewModel: NotificationSettingsViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        viewModel = KoinInitializerKt.getNotificationSettingsViewModel(
            userId: ConstantsKt.PROTOTYPE_USER_ID,
            onScheduleChanged: { hourNum, minuteNum, enabled in
                let hourInt = Int(truncating: hourNum)
                let minuteInt = Int(truncating: minuteNum)
                if enabled.boolValue {
                    MorningAdviceTask.schedule(hour: hourInt, minute: minuteInt)
                } else {
                    MorningAdviceTask.cancel()
                }
            }
        )

        // Snapshot inicial para renderizar sin parpadeo
        let initial = viewModel.uiState
        morningEnabled = initial.morningEnabled
        hour = Int(initial.morningHour)
        minute = Int(initial.morningMinute)

        // Observación reactiva — refleja cualquier cambio del ViewModel
        observeTask = Task {
            do {
                for try await state in asyncSequence(for: viewModel.uiStateFlow) {
                    self.morningEnabled = state.morningEnabled
                    self.hour = Int(state.morningHour)
                    self.minute = Int(state.morningMinute)
                }
            } catch { }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    func save() {
        viewModel.setHour(hour: Int32(hour))
        viewModel.setMinute(minute: Int32(minute))
        viewModel.toggleEnabled(enabled: morningEnabled)
        viewModel.save()
    }
}
