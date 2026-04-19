//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI

struct ContentView: View {
    init() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(white: 1.0, alpha: 1.0)
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView {
            DashboardView()
                .tabItem {
                    Label("Inicio", systemImage: "house.fill")
                }

            ChatView()
                .tabItem {
                    Label("Asistente", systemImage: "bubble.left.and.bubble.right.fill")
                }

            PantryView()
                .tabItem {
                    Label("Alacena", systemImage: "cabinet.fill")
                }

            // Placeholder
            ClinicalProfileView()
                .tabItem {
                    Label("Perfil", systemImage: "person.fill")
                }
        }
        .tint(TanayenTheme.primaryGreen)
        .background(TanayenTheme.background)
    }
}

struct PlaceholderView: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 12) {
            Text(title)
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .foregroundColor(TanayenTheme.textDark)
            Text(subtitle)
                .font(.system(.subheadline, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(TanayenTheme.background)
    }
}
