//
// Created by Carlo Huaman Torres on 6/03/26.
//
import SwiftUI

struct DashboardView: View {
    // TODO: conectar con ViewModel del shared module
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // Saludo
                VStack(alignment: .leading, spacing: 4) {
                    Text(greetingByHour())
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                    Text("Carlo")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(TanayenTheme.textDark)
                    Text("Tu VFC está baja hoy — tómatelo con calma 🌿")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }
                .padding(.horizontal, 24)
                .padding(.top, 16)

                // Alerta
                AlertBannerView(
                    emoji: "😴",
                    message: "Dormiste 5.1h. Evita cafeína después de las 14:00."
                )
                    .padding(.horizontal, 24)

                // Métricas — fila 1
                HStack(spacing: 12) {
                    MetricCardView(emoji: "🌙", value: "5.1", unit: "h",
                                   label: "Sueño", tint: TanayenTheme.accentTerra)
                    MetricCardView(emoji: "💚", value: "42", unit: "ms",
                                   label: "VFC", tint: TanayenTheme.secondaryMint)
                }
                .padding(.horizontal, 24)

                // Métricas — fila 2
                HStack(spacing: 12) {
                    MetricCardView(emoji: "⚖️", value: "78.2", unit: "kg",
                                   label: "Peso", tint: TanayenTheme.secondaryMint)
                    MetricCardView(emoji: "❤️", value: "68", unit: "bpm",
                                   label: "FC reposo", tint: Color(hex: "#E63946"))
                }
                .padding(.horizontal, 24)

                // Lo que comiste hoy
                TodayFoodCardView(
                    foodLogs: [("Desayuno", "Avena con almendras"),
                               ("Almuerzo", "Ensalada con atún")]
                )
                    .padding(.horizontal, 24)

                // CTA
                NavigationLink(destination: ChatView()) {
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
