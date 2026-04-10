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
    var hasAttachedImage: Bool = false
    var pantrySuggestion: PantrySuggestionWrapper?
}

struct PantrySuggestionWrapper: Equatable {
    let ingredients: [String]
    let confirmed: Bool

    init(from shared: Shared.PantrySuggestion) {
        self.ingredients = shared.ingredients
        self.confirmed = shared.confirmed
    }
}

private enum ImageSource: Identifiable {
    case camera, gallery
    var id: Self { self }
}

struct ChatView: View {
    @StateObject private var chatVM = ChatViewModelWrapper()
    @State private var inputText = ""
    @State private var imageSource: ImageSource?

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
                            VStack(spacing: 4) {
                                ChatBubbleView(message: message)
                                    .id(message.id)

                                if let suggestion = message.pantrySuggestion {
                                    PantrySuggestionChipView(
                                        suggestion: suggestion,
                                        onConfirm: { chatVM.confirmPantrySuggestion(messageId: message.id) },
                                        onDismiss: { chatVM.dismissPantrySuggestion(messageId: message.id) }
                                    )
                                }
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }
                .scrollDismissesKeyboard(.interactively)
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

            // Preview de Imagen antes de enviar
            if let pendingBase64 = chatVM.pendingImageBase64 {
                PendingImagePreviewView(base64: pendingBase64) {
                    chatVM.clearPendingImage()
                }
            }

            // Input
            ChatInputBarView(
                text: $inputText,
                isLoading: chatVM.isLoading,
                hasPendingImage: chatVM.pendingImageBase64 != nil,
                onCameraClick: { imageSource = .camera },
                onGalleryClick: { imageSource = .gallery },
                onSend: {
                    let text = inputText.trimmingCharacters(in: .whitespaces)
                    guard !text.isEmpty || chatVM.pendingImageBase64 != nil else { return }
                    guard !chatVM.isLoading else { return }
                    chatVM.sendMessage(text)
                    inputText = ""
                }
            )
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Listo") {
                    UIApplication.shared.sendAction(
                        #selector(UIResponder.resignFirstResponder),
                        to: nil, from: nil, for: nil
                    )
                }
                .foregroundColor(TanayenTheme.primaryGreen)
            }
        }
        .sheet(item: $imageSource) { source in
            ImagePicker(sourceType: source == .camera ? .camera : .photoLibrary) { image in
                if let image = image, let base64 = image.resizeAndEncode(maxSize: 800) {
                    chatVM.attachImage(base64: base64)
                }
            }
        }
    }
}
