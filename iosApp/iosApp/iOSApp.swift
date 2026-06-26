import SwiftUI
import UserNotifications
import Shared

@main
struct IOSApp: App {

    init() {
        // Register BGTask before app finishes launching
        MorningAdviceTask.register()

        let rawHost = Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? ""
        let url = "https://\(rawHost)"
        let key = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""
        let bundleGeminiKey = Bundle.main.infoDictionary?["GEMINI_API_KEY"] as? String ?? ""
        let effectiveGeminiKey = KeychainHelper.load() ?? bundleGeminiKey
        print("=== iOS Supabase URL: \(url)")
        KoinInitializerKt.doInitKoin(
            supabaseUrl: url,
            supabaseAnonKey: key,
            geminiApiKey: effectiveGeminiKey
        )

        // Request notification permission and schedule based on saved prefs
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        let prefs = KoinInitializerKt.getNotificationPrefs()
        let settings = prefs.load(userId: ConstantsKt.PROTOTYPE_USER_ID)
        if settings.morningEnabled {
            MorningAdviceTask.schedule(hour: Int(settings.morningHour), minute: Int(settings.morningMinute))
        }
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
