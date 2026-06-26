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
                let fraction = CGFloat(min(max(1.0 - hrv / 100.0, 0), 1))

                ZStack(alignment: .leading) {
                    // Fondo gradiente
                    RoundedRectangle(cornerRadius: height / 2)
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    TanayenTheme.secondaryMint,
                                    Color(hex: "#FFB703"),
                                    Color(hex: "#E63946")]),
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

struct NutritionSummaryCardView: View {
    let nutrition: NutritionSummaryData

    private let orangeDark = Color(hex: "#F4A261")
    private let blueMint = Color(hex: "#74C69D")
    private let blueLight = Color(hex: "#74B9FF")

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Nutrición hoy")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundColor(TanayenTheme.textDark)
                Spacer()
                Text("\(nutrition.mealCount) comida\(nutrition.mealCount != 1 ? "s" : "")")
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
            }

            // Calorías y barra de progreso
            VStack(spacing: 6) {
                HStack {
                    Text("\(Int(nutrition.totalCalories)) kcal")
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .foregroundColor(TanayenTheme.primaryGreen)
                    Spacer()
                    Text("/\(Int(nutrition.calorieGoal)) objetivo")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }

                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color(hex: "#EEEEEE"))
                            .frame(height: 8)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(progressColor)
                            .frame(width: geometry.size.width * CGFloat(nutrition.calorieProgress), height: 8)
                    }
                }
                .frame(height: 8)

                if nutrition.remainingCalories > 0 {
                    Text("Quedan \(Int(nutrition.remainingCalories)) kcal para hoy")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            // Macros
            HStack {
                Spacer()
                MacroItemView(label: "Proteína", value: "\(Int(nutrition.totalProteinG))g", color: blueMint)
                Spacer()
                MacroItemView(label: "Carbos", value: "\(Int(nutrition.totalCarbsG))g", color: blueLight)
                Spacer()
                MacroItemView(label: "Grasa", value: "\(Int(nutrition.totalFatG))g", color: orangeDark)
                Spacer()
            }

            Divider()

            // Micronutrientes
            HStack {
                Spacer()
                MicroItemView(label: "Fibra", value: "\(Int(nutrition.totalFiberG))g")
                Spacer()
                MicroItemView(label: "Sodio", value: "\(Int(nutrition.totalSodiumMg))mg")
                Spacer()
                MicroItemView(label: "Azúcar", value: "\(Int(nutrition.totalSugarG))g")
                Spacer()
            }
        }
        .padding(16)
        .background(TanayenTheme.surface)
        .cornerRadius(20)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }

    private var progressColor: Color {
        if nutrition.calorieProgress < 0.5 { return blueMint }
        if nutrition.calorieProgress < 0.9 { return TanayenTheme.primaryGreen }
        return orangeDark
    }
}

struct MacroItemView: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(.subheadline, design: .rounded, weight: .bold))
                .foregroundColor(color)
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
        }
    }
}

struct MicroItemView: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(.subheadline, design: .rounded, weight: .semibold))
                .foregroundColor(TanayenTheme.textDark)
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundColor(TanayenTheme.textMuted)
        }
    }
}

struct TodayFoodCardView: View {
    let foodLogs: [(String, String)]
    let onAddClick: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Hoy comiste")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundColor(TanayenTheme.textDark)
                Spacer()
                Button("+ Agregar", action: onAddClick)
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
