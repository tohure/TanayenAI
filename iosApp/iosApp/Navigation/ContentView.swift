//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI

struct ContentView: View {
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

            // Placeholder
            PlaceholderView(title: "Alacena", subtitle: "Disponible próximamente")
                .tabItem {
                    Label("Alacena", systemImage: "cabinet.fill")
                }

            // Placeholder
            PlaceholderView(title: "Perfil", subtitle: "Disponible próximamente")
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
