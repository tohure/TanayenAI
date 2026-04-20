//
// Created by Carlo Huaman Torres on 6/03/26.
//
import SwiftUI

struct MetricCardView: View {
    let emoji: String
    let value: String
    let unit: String
    let label: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji).font(.title2)
            HStack(alignment: .bottom, spacing: 2) {
                Text(value)
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(tint)
                Text(unit)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
                    .padding(.bottom, 2)
            }
            Text(label)
                .font(.system(.caption, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(TanayenTheme.surface)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

struct StressLevelCardView: View {
    let hrvValue: String

    private var hrv: Float {
        Float(hrvValue) ?? 0
    }

    private var stressText: String {
        if hrvValue == "--" { return "--" }
        if hrv < 40 { return "Alto" }
        if hrv < 60 { return "Medio" }
        return "Bajo"
    }

    private var stressColor: Color {
        if hrvValue == "--" { return TanayenTheme.textMuted }
        if hrv < 40 { return Color(hex: "#E63946") }
        if hrv < 60 { return Color(hex: "#FFB703") }
        return TanayenTheme.secondaryMint
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Nivel de estrés")
                .font(.system(.caption, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)

            Text(stressText)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundColor(stressColor)

            Spacer().frame(height: 4)

            // Barra Semaforizada
            GeometryReader { geometry in
                let width = geometry.size.width
                let height: CGFloat = 12
                let fraction = CGFloat(min(max(hrv / 100.0, 0), 1))

                ZStack(alignment: .leading) {
                    // Fondo gradiente
                    RoundedRectangle(cornerRadius: height / 2)
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color(hex: "#E63946"),
                                    Color(hex: "#FFB703"),
                                    TanayenTheme.secondaryMint]),
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(height: height)

                    // Marcador Circular
                    if hrvValue != "--" {
                        let rawX = width * fraction
                        let safeX = min(max(rawX, height / 2), width - height / 2)

                        Circle()
                            .fill(Color.white)
                            .frame(width: height, height: height)
                            .overlay(
                                Circle().stroke(Color.gray, lineWidth: 1)
                            )
                            .position(x: safeX, y: height / 2)
                    }
                }
            }
            .frame(height: 12)

            Spacer().frame(height: 4)

            Text("VFC: \(hrvValue) ms")
                .font(.system(size: 11, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(TanayenTheme.surface)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

struct AlertBannerView: View {
    let emoji: String
    let message: String

    var body: some View {
        HStack(spacing: 10) {
            Text(emoji).font(.body)
            Text(message)
                .font(.system(.subheadline, design: .rounded))
                .foregroundColor(TanayenTheme.accentTerra)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#FFF3E0"))
        .cornerRadius(12)
    }
}

struct TodayFoodCardView: View {
    let foodLogs: [(String, String)]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Hoy comiste")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundColor(TanayenTheme.textDark)
                Spacer()
                // Botón dummy — funcionalidad en fase futura
                Button("+ Agregar") {}
                    .font(.system(.caption, design: .rounded, weight: .medium))
                    .foregroundColor(TanayenTheme.primaryGreen)
            }

            if foodLogs.isEmpty {
                Text("Cuéntale al asistente qué comiste 🍽️")
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
            } else {
                ForEach(foodLogs, id: \.0) { mealType, foodName in
                    HStack {
                        Text(mealType)
                            .font(.system(.caption, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)
                        Spacer()
                        Text(foodName)
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textDark)
                    }
                }
            }
        }
        .padding(16)
        .background(TanayenTheme.surface)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}
