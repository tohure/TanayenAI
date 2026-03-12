//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI

struct ChatBubbleView: View {
    let message: ChatMessage

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if !message.isUser {
                // Avatar asistente
                ZStack {
                    Circle()
                        .fill(Color(hex: "#E8F5EE"))
                        .frame(width: 32, height: 32)
                    Text("🌿").font(.system(size: 16))
                }
            }

            if message.isLoading {
                TypingIndicatorView()
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(TanayenTheme.surface)
                    .cornerRadius(16, corners: [.topLeft, .topRight, .bottomRight])
            } else {
                Text(message.content)
                    .font(.system(.body, design: .rounded))
                    .foregroundColor(message.isUser ? .white : TanayenTheme.textDark)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(message.isUser ? TanayenTheme.primaryGreen : TanayenTheme.surface)
                    .cornerRadius(
                        16,
                        corners: message.isUser
                            ? [.topLeft, .topRight, .bottomLeft]
                            : [.topLeft, .topRight, .bottomRight]
                    )
                    .frame(maxWidth: 280, alignment: message.isUser ? .trailing : .leading)
            }

            if message.isUser { Spacer() }
        }
        .frame(maxWidth: .infinity, alignment: message.isUser ? .trailing : .leading)
        .padding(.horizontal, 16)
    }
}

struct TypingIndicatorView: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 5) {
            ForEach(0..<3) { interval in
                Circle()
                    .fill(TanayenTheme.textMuted)
                    .frame(width: 7, height: 7)
                    .offset(y: animating ? -6 : 0)
                    .animation(
                        Animation
                            .easeInOut(duration: 0.4)
                            .repeatForever(autoreverses: true)
                            .delay(Double(interval) * 0.15),
                        value: animating
                    )
            }
        }
        .padding(.vertical, 6)
        .onAppear { animating = true }
        .onDisappear { animating = false }
    }
}

struct ChatInputBarView: View {
    @Binding var text: String
    let isLoading: Bool
    let onCameraClick: () -> Void
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            // Botón cámara
            Button(action: onCameraClick) {
                Text("📷")
                    .font(.system(size: 18))
                    .frame(width: 40, height: 40)
                    .background(Color(hex: "#E8F5EE"))
                    .clipShape(Circle())
            }

            // Campo de texto
            TextField("Escribe o pregunta algo...", text: $text, axis: .vertical)
                .font(.system(.body, design: .rounded))
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(TanayenTheme.background)
                .cornerRadius(24)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(Color(hex: "#E0E0E0"), lineWidth: 1)
                )
                .lineLimit(1...4)
                .onSubmit { if !isLoading { onSend() } }

            // Botón enviar
            Button(action: onSend) {
                Text("↑")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(
                        text.trimmingCharacters(in: .whitespaces).isEmpty || isLoading
                            ? Color(hex: "#E0E0E0")
                            : TanayenTheme.primaryGreen
                    )
                    .clipShape(Circle())
            }
            .disabled(text.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(TanayenTheme.surface)
    }
}

// Helper para cornerRadius por esquinas específicas
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
