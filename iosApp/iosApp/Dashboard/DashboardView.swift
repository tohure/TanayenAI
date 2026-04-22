//
// Created by Carlo Huaman Torres on 6/03/26.
//
import SwiftUI
import Shared

struct DashboardView: View {
    var onNavigateToChat: () -> Void = {}
    @StateObject private var viewmodel = DashboardViewModelWrapper()
    @State private var showNotificationSettings = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // Saludo
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(greetingByHour())
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)
                        Text("Carlo")
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .foregroundColor(TanayenTheme.textDark)
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

                // Métricas — fila 1
                HStack(spacing: 12) {
                    MetricCardView(emoji: "🌙", value: viewmodel.sleepHours, unit: "h",
                                   label: "Sueño", tint: TanayenTheme.accentTerra)
                    StressLevelCardView(hrvValue: viewmodel.hrv)
                }
                .padding(.horizontal, 24)

                // Métricas — fila 2
                HStack(spacing: 12) {
                    MetricCardView(emoji: "⚖️", value: viewmodel.weightKg, unit: "kg",
                                   label: "Peso", tint: TanayenTheme.secondaryMint)
                    MetricCardView(emoji: "🔥", value: viewmodel.caloriesBurned, unit: "kcal",
                                   label: "Calorías activas", tint: Color(hex: "#E63946"))
                }
                .padding(.horizontal, 24)

                // Lo que comiste hoy
                TodayFoodCardView(
                    foodLogs: viewmodel.foodLogs,
                    onAddClick: onNavigateToChat
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
            // Recarga inmediata — no esperar permisos de salud
            viewmodel.load()
            // Sincronización de salud en paralelo
            KoinInitializerKt.requestHealthPermissionsFromIos { _ in
                DispatchQueue.main.async {
                    KoinInitializerKt.triggerSyncFromIos()
                }
            }
        }
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
