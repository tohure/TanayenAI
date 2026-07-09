//
// Created by Carlo Huaman Torres on 6/03/26.
//
import SwiftUI
import Shared

struct DashboardView: View {
    var onNavigateToChat: () -> Void = {}
    @StateObject private var viewmodel = DashboardViewModelWrapper()
    @State private var showNotificationSettings = false
    @State private var showFoodDiary = false

    var body: some View {
        NavigationStack {
            content
                .navigationDestination(isPresented: $showFoodDiary) {
                    FoodDiaryView()
                }
                .onChangeCompat(of: showFoodDiary) { presented in
                    // Al volver del Diario, recalcula la nutrición por si se borró un registro.
                    if !presented { viewmodel.refreshNutrition() }
                }
        }
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // Saludo
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(greetingByHour())
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)
                        Text(viewmodel.userName.isEmpty ? "Hola" : viewmodel.userName)
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .foregroundColor(TanayenTheme.textDark)
                            .onTapGesture(count: 3) {
                                viewmodel.requestEditName()
                            }
                        Text(viewmodel.alerts.first ?? "Todo bien por ahora 🌿")
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)
                    }
                    Spacer()
                    Button {
                        showNotificationSettings = true
                    } label: {
                        Image(systemName: "bell")
                            .font(.system(size: 20))
                            .foregroundColor(TanayenTheme.textMuted)
                            .frame(width: 40, height: 40)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 16)
                .sheet(isPresented: $showNotificationSettings) {
                    NotificationSettingsView()
                }

                // Alertas dinámicas
                if let firstAlert = viewmodel.alerts.first {
                    AlertBannerView(
                        emoji: "⚠️",
                        message: firstAlert
                    )
                    .padding(.horizontal, 24)
                }

                // Métricas — solo las que tienen dato; de a 2 por fila. Sin datos → nada.
                ForEach(Array(metricCards.chunked(into: 2).enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 12) {
                        ForEach(0..<row.count, id: \.self) { idx in row[idx] }
                        if row.count == 1 { Spacer() }
                    }
                    .padding(.horizontal, 24)
                }

                // Lo que comiste hoy
                TodayFoodCardView(
                    foodLogs: viewmodel.foodLogs,
                    onAddClick: onNavigateToChat,
                    onViewAll: { showFoodDiary = true }
                )
                .padding(.horizontal, 24)

                // Resumen nutricional
                if let nutrition = viewmodel.todayNutrition {
                    NutritionSummaryCardView(nutrition: nutrition)
                        .padding(.horizontal, 24)
                }

                // CTA
                Button(action: onNavigateToChat) {
                    HStack {
                        Spacer()
                        Text("¿Qué como hoy? 🌿")
                            .font(.system(.headline, design: .rounded, weight: .semibold))
                            .foregroundColor(.white)
                        Spacer()
                    }
                    .frame(height: 52)
                    .background(TanayenTheme.primaryGreen)
                    .cornerRadius(16)
                }
                .padding(.horizontal, 24)

                Spacer(minLength: 20)
            }
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
        .onAppear {
            viewmodel.load()
            KoinInitializerKt.requestHealthPermissionsFromIos { _ in }
        }
        .sheet(isPresented: $viewmodel.showNameDialog, onDismiss: {
            viewmodel.dismissNameDialog()
        }) {
            NameInputView(
                initialName: viewmodel.rawDisplayName,
                onSave: { name in viewmodel.saveDisplayName(name) },
                onSkip: { viewmodel.dismissNameDialog() }
            )
            .presentationDetents([.height(280)])
        }
    }

    /// Cards de métricas con dato real ("--" = sin dato → se omite).
    private var metricCards: [AnyView] {
        var cards: [AnyView] = []
        if viewmodel.sleepHours != "--" {
            cards.append(AnyView(MetricCardView(emoji: "🌙", value: viewmodel.sleepHours, unit: "h",
                                                label: "Sueño", tint: TanayenTheme.accentTerra)))
        }
        if viewmodel.hrv != "--" {
            cards.append(AnyView(StressLevelCardView(hrvValue: viewmodel.hrv)))
        }
        if viewmodel.weightKg != "--" {
            cards.append(AnyView(MetricCardView(emoji: "⚖️", value: viewmodel.weightKg, unit: "kg",
                                                label: "Peso", tint: TanayenTheme.secondaryMint)))
        }
        if viewmodel.caloriesBurned != "--" {
            cards.append(AnyView(MetricCardView(emoji: "🔥", value: viewmodel.caloriesBurned, unit: "kcal",
                                                label: "Calorías quemadas", tint: Color(hex: "#E63946"))))
        }
        return cards
    }

    private func greetingByHour() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5...11:  return "Buenos días ☀️"
        case 12...17: return "Buenas tardes 🌤️"
        default:      return "Buenas noches 🌙"
        }
    }
}

private extension Array {
    /// Parte el array en sub-arrays de tamaño `size` (para acomodar las cards de a 2 por fila).
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map { Array(self[$0 ..< Swift.min($0 + size, count)]) }
    }
}
