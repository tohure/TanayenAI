//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI
import UIKit

struct ContentView: View {
    init() {
        // ── Tab bar ──────────────────────────────────────────────────────────
        let tabAppearance = UITabBarAppearance()
        tabAppearance.configureWithOpaqueBackground()
        tabAppearance.backgroundColor = UIColor(white: 1.0, alpha: 1.0)
        UITabBar.appearance().standardAppearance = tabAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabAppearance

        // ── Navigation bar (fixes white title in all modal sheets) ───────────
        // #F7F6F3 background, #1A1A2E title, #2D6A4F buttons
        let navBgColor = UIColor(red: 247 / 255.0, green: 246 / 255.0, blue: 243 / 255.0, alpha: 1)
        let navTitleColor = UIColor(red: 26 / 255.0, green: 26 / 255.0, blue: 46 / 255.0, alpha: 1)
        let navTintColor = UIColor(red: 45 / 255.0, green: 106 / 255.0, blue: 79 / 255.0, alpha: 1)
        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithOpaqueBackground()
        navAppearance.backgroundColor = navBgColor
        navAppearance.titleTextAttributes = [.foregroundColor: navTitleColor]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance
        UINavigationBar.appearance().compactAppearance = navAppearance
        UINavigationBar.appearance().tintColor = navTintColor
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
