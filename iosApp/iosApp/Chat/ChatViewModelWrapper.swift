//
// Created by Carlo Huaman Torres on 12/03/26.
//

import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ChatViewModelWrapper: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var contextReady = false
    @Published var error: String? = nil
    @Published var pendingImageBase64: String? = nil

    // ViewModel compartido de KMP
    private let chatVM: ChatViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        let userId = ConstantsKt.PROTOTYPE_USER_ID // Dummy
        self.chatVM = KoinInitializerKt.getChatViewModel(userId: userId)

        observeTask = Task {
            do {
                for try await state in asyncSequence(for: chatVM.uiStateFlow) {
                    observeState(state)
                }
            } catch {
                print("Error observing state: \(error)")
            }
        }
    }

    private func observeState(_ state: ChatUiState) {
        self.messages = state.messages.map { msg in
            ChatMessage(
                id: msg.id,
                content: msg.content,
                isUser: msg.isUser,
                isLoading: msg.isLoading,
                hasAttachedImage: msg.hasAttachedImage,
                pantrySuggestion: msg.pantrySuggestion.map { PantrySuggestionWrapper(from: $0) }
            )
        }
        self.isLoading = state.isLoading
        self.contextReady = state.contextReady
        self.error = state.error
        self.pendingImageBase64 = state.pendingImage?.base64Data
    }

    func attachImage(base64: String, mimeType: String = "image/jpeg") {
        chatVM.attachImage(base64: base64, mimeType: mimeType)
    }

    func clearPendingImage() {
        chatVM.clearPendingImage()
    }

    func confirmPantrySuggestion(messageId: String) {
        chatVM.confirmPantrySuggestion(messageId: messageId)
    }

    func dismissPantrySuggestion(messageId: String) {
        chatVM.dismissPantrySuggestion(messageId: messageId)
    }

    func sendMessage(_ text: String) {
        chatVM.sendMessage(userText: text)
    }
}
