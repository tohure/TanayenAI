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

    // ViewModel compartido de KMP
    private let chatVM: ChatViewModel
    private var observeTask: Task<Void, Never>?

    init() {
        let userId = "00000000-0000-0000-0000-000000000001" // Dummy
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
                isLoading: msg.isLoading
            )
        }
        self.isLoading = state.isLoading
        self.contextReady = state.contextReady
        self.error = state.error
    }

    func sendMessage(_ text: String) {
        chatVM.sendMessage(userText: text)
    }
}
