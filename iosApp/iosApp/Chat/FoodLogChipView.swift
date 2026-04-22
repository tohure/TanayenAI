//
// Created by Carlo Huaman Torres on 19/04/26.
//

import SwiftUI

struct FoodLogSuggestionChipView: View {
    let description: String
    let confirmed: Bool
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    private let orangeLight = Color(hex: "#FFF3E0")
    private let orangeDark = Color(hex: "#F4A261")

    var body: some View {
        if confirmed {
            Text("✓ Registrado en tu diario")
                .font(.system(.caption, design: .rounded, weight: .medium))
                .foregroundColor(orangeDark)
                .padding(.leading, 48)
                .padding(.top, 4)
        } else {
            HStack(spacing: 8) {
                Text("¿Registro \"\(description)\"?")
                    .font(.system(.caption, design: .rounded))
                    .foregroundColor(orangeDark)
                Spacer()
                Button("No", action: onDismiss)
                    .font(.system(.caption, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
                Button(action: onConfirm) {
                    Text("Sí")
                        .font(.system(.caption, design: .rounded, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(orangeDark)
                        .cornerRadius(8)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(orangeLight)
            .cornerRadius(12)
            .padding(.leading, 48)
            .padding(.top, 4)
            .padding(.trailing, 16)
        }
    }
}

enum CheckInUserResponse {
    case pending, yes, no
}

struct CheckInChipView: View {
    let mealType: String
    let recommendedFood: String
    let userResponse: CheckInUserResponse
    let onYes: () -> Void
    let onNo: () -> Void

    private let orangeLight = Color(hex: "#FFF3E0")
    private let orangeDark = Color(hex: "#F4A261")

    var body: some View {
        switch userResponse {
        case .yes:
            Text("✓ Perfecto, registrado")
                .font(.system(.caption, design: .rounded, weight: .medium))
                .foregroundColor(orangeDark)
                .padding(.leading, 48)
                .padding(.top, 4)
        case .no:
            EmptyView()
        case .pending:
            HStack(spacing: 8) {
                Text("¿Lo comiste?")
                    .font(.system(.caption, design: .rounded))
                    .foregroundColor(orangeDark)
                Spacer()
                Button(action: onNo) {
                    Text("No")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(orangeDark)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(orangeDark, lineWidth: 1)
                        )
                }
                Button(action: onYes) {
                    Text("Sí")
                        .font(.system(.caption, design: .rounded, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(orangeDark)
                        .cornerRadius(8)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(orangeLight)
            .cornerRadius(12)
            .padding(.leading, 48)
            .padding(.top, 4)
            .padding(.trailing, 16)
        }
    }
}
