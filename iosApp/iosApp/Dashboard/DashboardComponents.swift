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
