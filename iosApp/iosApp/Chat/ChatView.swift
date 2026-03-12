//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI
import Shared

struct ChatMessage: Identifiable {
    let id: String
    let content: String
    let isUser: Bool
    var isLoading: Bool = false
}

struct ChatView: View {
    @StateObject private var chatVM = ChatViewModelWrapper()
    @State private var inputText = ""

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(alignment: .leading, spacing: 4) {
                Text("Asistente")
                    .font(.system(size: 22, weight: .semibold, design: .rounded))
                    .foregroundColor(TanayenTheme.textDark)
                Text(chatVM.contextReady ? "Contexto listo 🌿" : "Cargando contexto...")
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
                        ForEach(chatVM.messages) { message in
                            ChatBubbleView(message: message)
                                .id(message.id)
                        }
                    }
                    .padding(.vertical, 8)
                }
                .onChange(of: chatVM.messages.last?.content) { _, _ in
                    if let last = chatVM.messages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            if let error = chatVM.error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding(.horizontal)
            }

            // Input
            ChatInputBarView(
                text: $inputText,
                isLoading: chatVM.isLoading,
                onCameraClick: {
                    // TODO: cámara para alacena
                },
                onSend: {
                    let text = inputText.trimmingCharacters(in: .whitespaces)
                    guard !text.isEmpty, !chatVM.isLoading else { return }
                    chatVM.sendMessage(text)
                    inputText = ""
                }
            )
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
    }
}

@MainActor
class ChatViewModelWrapper: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var contextReady = false
    @Published var error: String? = nil

    private let viewModel: ChatViewModel

    init() {
        self.viewModel = KoinInitializerKt.getChatViewModel(userId: "00000000-0000-0000-0000-000000000001")
        observeState()
    }

    private func observeState() {
        viewModel.observeUiState { [weak self] state in
            guard let self = self else { return }

            DispatchQueue.main.async {
                self.messages = state.messages.map { msg in
                    ChatMessage(
                        id: msg.id,
                        content: msg.content,
                        isUser: msg.isUser,
                        isLoading: msg.isLoading
                    )
                }
                self.isLoading = state.isLoading
                self.contextReady = state.contextReady
                self.error = state.error
            }
        }
    }

    func sendMessage(_ text: String) {
        viewModel.sendMessage(userText: text)
    }
}
