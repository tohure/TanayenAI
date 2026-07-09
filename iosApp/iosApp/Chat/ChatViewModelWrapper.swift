//
// Created by Carlo Huaman Torres on 12/03/26.
//

import Shared
import KMPNativeCoroutinesAsync

struct FoodLogSuggestionData: Equatable, Identifiable {
    let id: String
    let description: String
    let confirmed: Bool
    let isLoading: Bool

    init(from shared: Shared.FoodLogSuggestion) {
        self.id = shared.id
        self.description = shared.description_
        self.confirmed = shared.confirmed
        self.isLoading = shared.isLoading
    }
}

struct CheckInSuggestionData: Equatable {
    let mealType: String
    let recommendedFood: String
    let userResponse: CheckInUserResponse
    let isLoading: Bool

    init(from shared: Shared.CheckInSuggestion) {
        self.mealType = shared.mealType
        self.recommendedFood = shared.recommendedFood
        self.isLoading = shared.isLoading
        switch shared.userResponse {
        case .yes:
            self.userResponse = .yes
        case .no:
            self.userResponse = .no
        default:
            self.userResponse = .pending
        }
    }
}

struct ChatMessage: Identifiable, Equatable {
    let id: String
    let content: String
    let isUser: Bool
    var isLoading: Bool = false
    var hasAttachedImage: Bool = false
    var pantrySuggestion: PantrySuggestionWrapper?
    var foodLogSuggestions: [FoodLogSuggestionData] = []
    var checkInSuggestion: CheckInSuggestionData?
}

struct PantrySuggestionWrapper: Equatable {
    let ingredients: [String]
    let confirmed: Bool
    let isLoading: Bool

    init(from shared: Shared.PantrySuggestion) {
        self.ingredients = shared.ingredients
        self.confirmed = shared.confirmed
        self.isLoading = shared.isLoading
    }
}

private enum ImageSource: Identifiable {
    case camera, gallery
    var id: Self { self }
}

@MainActor
class ChatViewModelWrapper: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var contextReady = false
    @Published var error: String?
    @Published var pendingImagesBase64: [String] = []

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

    deinit {
        observeTask?.cancel()
    }

    private func observeState(_ state: ChatUiState) {
        // Solo reasignar cuando cambia — evita re-render de SwiftUI en cada emisión del flow.
        let newMessages = state.messages.map { msg in
            ChatMessage(
                id: msg.id,
                content: msg.content,
                isUser: msg.isUser,
                isLoading: msg.isLoading,
                hasAttachedImage: msg.hasAttachedImage,
                pantrySuggestion: msg.pantrySuggestion.map { PantrySuggestionWrapper(from: $0) },
                foodLogSuggestions: msg.foodLogSuggestions.map { FoodLogSuggestionData(from: $0) },
                checkInSuggestion: msg.checkInSuggestion.map { CheckInSuggestionData(from: $0) }
            )
        }
        if newMessages != self.messages { self.messages = newMessages }
        if isLoading != state.isLoading { isLoading = state.isLoading }
        if contextReady != state.contextReady { contextReady = state.contextReady }
        if error != state.error { error = state.error }
        let newImages = state.pendingImages.map { $0.base64Data }
        if pendingImagesBase64 != newImages { pendingImagesBase64 = newImages }
    }

    func attachImage(base64: String, mimeType: String = "image/jpeg") {
        chatVM.attachImage(base64: base64, mimeType: mimeType)
    }

    func removePendingImage(index: Int) {
        chatVM.removePendingImage(index: Int32(index))
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

    func confirmFoodLogSuggestion(messageId: String, suggestionId: String) {
        chatVM.confirmFoodLogSuggestion(messageId: messageId, suggestionId: suggestionId)
    }

    func dismissFoodLogSuggestion(messageId: String, suggestionId: String) {
        chatVM.dismissFoodLogSuggestion(messageId: messageId, suggestionId: suggestionId)
    }

    func confirmCheckInYes(messageId: String) {
        chatVM.confirmCheckInYes(messageId: messageId)
    }

    func confirmCheckInNo(messageId: String) {
        chatVM.confirmCheckInNo(messageId: messageId)
    }

    func sendMessage(_ text: String) {
        chatVM.sendMessage(userText: text)
    }
}
