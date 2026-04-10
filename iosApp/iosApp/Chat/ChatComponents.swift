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
                VStack(alignment: message.isUser ? .trailing : .leading, spacing: 4) {
                    if message.hasAttachedImage {
                        Text("📷 Imagen procesada ✓")
                            .font(.system(size: 11, design: .rounded))
                            .foregroundColor(message.isUser ? .white.opacity(0.8) : TanayenTheme.textMuted)
                            .padding(.bottom, 2)
                    }
                    if !message.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text(message.content)
                            .font(.system(.body, design: .rounded))
                            .foregroundColor(message.isUser ? .white : TanayenTheme.textDark)
                    }
                }
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

struct PantrySuggestionChipView: View {
    let suggestion: PantrySuggestionWrapper
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        if suggestion.confirmed {
            Text("✓ Guardado en tu alacena")
                .font(.system(size: 12, weight: .medium, design: .rounded))
                .foregroundColor(TanayenTheme.secondaryMint)
                .padding(.leading, 48)
                .padding(.top, 4)
                .padding(.bottom, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            HStack(spacing: 8) {
                Text("¿Agrego \(suggestion.ingredients.count) ingrediente(s) a tu alacena?")
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(TanayenTheme.primaryGreen)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: onDismiss) {
                    Text("No")
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }
                .padding(.horizontal, 8)

                Button(action: onConfirm) {
                    Text("Sí")
                        .font(.system(size: 12, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(TanayenTheme.primaryGreen)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(hex: "#E8F5EE"))
            .cornerRadius(12)
            .padding(.leading, 48)
            .padding(.trailing, 16)
            .padding(.top, 4)
            .padding(.bottom, 8)
        }
    }
}

struct PendingImagePreviewView: View {
    let base64: String
    let onRemove: () -> Void

    var decodedImage: UIImage? {
        guard let data = Data(base64Encoded: base64, options: .ignoreUnknownCharacters) else { return nil }
        return UIImage(data: data)
    }

    var body: some View {
        HStack {
            if let image = decodedImage {
                ZStack(alignment: .topTrailing) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 72, height: 72)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button(action: onRemove) {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 20, height: 20)
                            .background(Color(hex: "#E63946"))
                            .clipShape(Circle())
                    }
                    .offset(x: 6, y: -6)
                }
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
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
    let hasPendingImage: Bool
    let onCameraClick: () -> Void
    let onGalleryClick: () -> Void
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onCameraClick) {
                Text("📷")
                    .font(.system(size: 18))
                    .frame(width: 40, height: 40)
                    .background(Color(hex: "#E8F5EE"))
                    .clipShape(Circle())
            }

            Button(action: onGalleryClick) {
                Text("🖼️")
                    .font(.system(size: 18))
                    .frame(width: 40, height: 40)
                    .background(Color(hex: "#E8F5EE"))
                    .clipShape(Circle())
            }

            TextField("Escribe o pregunta algo...", text: $text, axis: .vertical)
                .font(.system(.body, design: .rounded))
                .foregroundColor(TanayenTheme.textDark)
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

            let isSendEnabled = (!text.trimmingCharacters(in: .whitespaces).isEmpty || hasPendingImage) && !isLoading

            Button(action: onSend) {
                Text("↑")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(isSendEnabled ? TanayenTheme.primaryGreen : Color(hex: "#E0E0E0"))
                    .clipShape(Circle())
            }
            .disabled(!isSendEnabled)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(TanayenTheme.surface)
    }
}

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
