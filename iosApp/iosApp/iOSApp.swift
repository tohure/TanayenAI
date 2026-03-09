import SwiftUI
import Shared

@main
struct IOSApp: App {

    init() {
        let rawHost = Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? ""
        let url = "https://\(rawHost)"
        let key = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""
        print("=== iOS Supabase URL: \(url)")
        KoinInitializerKt.doInitKoin(
            supabaseUrl: url,
            supabaseAnonKey: key
        )
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
