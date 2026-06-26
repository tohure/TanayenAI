import BackgroundTasks
import UserNotifications
import Shared

enum MorningAdviceTask {

    static let taskIdentifier = "dev.tohure.tanayenai.morning-advice"

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else { return }
            handleTask(refreshTask)
        }
    }

    static func schedule(hour: Int, minute: Int) {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskIdentifier)

        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = nextFireDate(hour: hour, minute: minute)
        try? BGTaskScheduler.shared.submit(request)
    }

    static func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskIdentifier)
    }

    // MARK: - Private

    private static func nextFireDate(hour: Int, minute: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = minute
        components.second = 0
        var date = Calendar.current.date(from: components) ?? Date()
        if date <= Date() {
            date = Calendar.current.date(byAdding: .day, value: 1, to: date) ?? date
        }
        return date
    }

    private static func handleTask(_ task: BGAppRefreshTask) {
        let useCase = KoinInitializerKt.getMorningAdviceUseCase(userId: ConstantsKt.PROTOTYPE_USER_ID)

        let workTask = Task {
            do {
                let advice = try await useCase.generate()
                if let advice = advice {
                    await showNotification(title: advice.title, body: advice.body)
                }
                // Mark complete BEFORE rescheduling to avoid duplicate registrations
                task.setTaskCompleted(success: true)
                let prefs = KoinInitializerKt.getNotificationPrefs()
                let settings = prefs.load(userId: ConstantsKt.PROTOTYPE_USER_ID)
                schedule(hour: Int(settings.morningHour), minute: Int(settings.morningMinute))
            } catch {
                task.setTaskCompleted(success: false)
            }
        }

        // Cancel the Swift task when iOS expires the BGTask — prevents leak
        task.expirationHandler = {
            workTask.cancel()
            task.setTaskCompleted(success: false)
        }
    }

    @MainActor
    private static func showNotification(title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        try? await UNUserNotificationCenter.current().add(request)
    }
}
