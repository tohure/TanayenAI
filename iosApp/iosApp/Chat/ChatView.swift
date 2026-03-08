//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI

struct ChatMessage: Identifiable {
    let id: String
    let content: String
    let isUser: Bool
    var isLoading: Bool = false
}

struct ChatView: View {
    @State private var messages: [ChatMessage] = [
        ChatMessage(
            id: "welcome",
            content: "Hola 🌿 Soy tu asistente nutricional. Puedes contarme qué comiste, mandarme fotos de tu alacena, o preguntarme qué comer hoy.",
            isUser: false
        )
    ]
    @State private var inputText = ""
    @State private var isLoading = false

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(alignment: .leading, spacing: 4) {
                Text("Asistente")
                    .font(.system(size: 22, weight: .semibold, design: .rounded))
                    .foregroundColor(TanayenTheme.textDark)
                Text("Contexto actualizado · hace 2 min")
                    .font(.system(.caption, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)

            Divider().opacity(0.3)

            // Mensajes
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(messages) { message in
                            ChatBubbleView(message: message)
                                .id(message.id)
                        }
                    }
                    .padding(.vertical, 8)
                }
                .onChange(of: messages.count) { _, _ in
                    if let last = messages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            // Input
            ChatInputBarView(
                text: $inputText,
                isLoading: isLoading,
                onCameraClick: {
                    // TODO: cámara para alacena
                },
                onSend: sendMessage
            )
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
    }

    private func sendMessage() {
        guard !inputText.trimmingCharacters(in: .whitespaces).isEmpty, !isLoading else { return }

        let text = inputText.trimmingCharacters(in: .whitespaces)
        messages.append(ChatMessage(id: UUID().uuidString, content: text, isUser: true))
        inputText = ""
        isLoading = true

        let loadingId = UUID().uuidString
        messages.append(ChatMessage(id: loadingId, content: "", isUser: false, isLoading: true))

        // TODO: reemplazar con Gemini API + ContextBuilder
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            messages.removeAll { $0.id == loadingId }
            messages.append(ChatMessage(
                id: UUID().uuidString,
                content: mockResponse(for: text),
                isUser: false
            ))
            isLoading = false
        }
    }

    private func mockResponse(for input: String) -> String {
        let lower = input.lowercased()
        if lower.contains("comer") {
            return "Basándome en tu alacena y métricas de hoy, te recomiendo una ensalada de atún con aguacate 🥗"
        } else if lower.contains("sueño") || lower.contains("dormí") {
            return "Veo que dormiste poco. Hoy evita cafeína después de las 14:00 🌙"
        } else {
            return "Entendido. ¿Quieres saber qué puedes comer hoy con lo que tienes en casa? 🌿"
        }
    }
}
