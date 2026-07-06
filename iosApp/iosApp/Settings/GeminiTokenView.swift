//
// Created by Carlo Huaman Torres on 22/04/26.
//

import SwiftUI

struct GeminiTokenView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var tokenText: String = KeychainHelper.load() ?? ""
    @State private var showToken = false
    @State private var saved = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {

                // ── Header ────────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("🌿 API Token")
                        .font(.system(size: 22, weight: .semibold, design: .rounded))
                        .foregroundColor(TanayenTheme.textDark)
                    Text("Token personal de Google AI Studio")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }

                // ── Info card ─────────────────────────────────────────────────
                Text(
                    "Cada usuario puede usar su propio API Key de Google AI Studio. " +
                    "Al guardar, se usará en el próximo arranque de la app. " +
                    "Déjalo vacío para usar el token por defecto."
                )
                .font(.system(.caption, design: .rounded))
                .foregroundColor(TanayenTheme.primaryGreen)
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "#E8F5EE"))
                .cornerRadius(12)

                // ── Campo ─────────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 6) {
                    Text("Gemini API Key")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)

                    HStack {
                        Group {
                            if showToken {
                                TextField("AIza...", text: $tokenText)
                            } else {
                                SecureField("AIza...", text: $tokenText)
                            }
                        }
                        .font(.system(.body, design: .monospaced))
                        .foregroundColor(TanayenTheme.textDark)
                        .onChangeCompat(of: tokenText) { _ in saved = false }

                        Button(action: { showToken.toggle() }) {
                            Text(showToken ? "🙈" : "👁️")
                        }
                    }
                    .padding(12)
                    .background(TanayenTheme.surface)
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(TanayenTheme.primaryGreen, lineWidth: 1)
                    )
                }

                if saved {
                    Text("✓ Guardado. Se usará al reiniciar la app.")
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(TanayenTheme.primaryGreen)
                }

                // ── Botones ───────────────────────────────────────────────────
                Button(action: saveToken) {
                    Text("Guardar")
                        .font(.system(.headline, design: .rounded, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(TanayenTheme.primaryGreen)
                        .cornerRadius(14)
                }

                Button(action: clearToken) {
                    Text("Usar token por defecto")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(TanayenTheme.textMuted.opacity(0.4), lineWidth: 1)
                        )
                }
            }
            .padding(24)
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
    }

    private func saveToken() {
        let trimmed = tokenText.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty {
            KeychainHelper.delete()
        } else {
            KeychainHelper.save(trimmed)
        }
        saved = true
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil, from: nil, for: nil
        )
    }

    private func clearToken() {
        KeychainHelper.delete()
        tokenText = ""
        saved = true
    }
}
