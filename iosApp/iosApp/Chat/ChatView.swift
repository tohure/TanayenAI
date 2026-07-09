//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI
import Shared

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

                                ForEach(message.foodLogSuggestions) { foodLog in
                                    FoodLogSuggestionChipView(
                                        description: foodLog.description,
                                        confirmed: foodLog.confirmed,
                                        isLoading: foodLog.isLoading,
                                        onConfirm: {
                                            chatVM.confirmFoodLogSuggestion(messageId: message.id, suggestionId: foodLog.id)
                                        },
                                        onDismiss: {
                                            chatVM.dismissFoodLogSuggestion(messageId: message.id, suggestionId: foodLog.id)
                                        }
                                    )
                                }

                                if let checkIn = message.checkInSuggestion {
                                    CheckInChipView(
                                        mealType: checkIn.mealType,
                                        recommendedFood: checkIn.recommendedFood,
                                        userResponse: checkIn.userResponse,
                                        isLoading: checkIn.isLoading,
                                        onYes: { chatVM.confirmCheckInYes(messageId: message.id) },
                                        onNo: { chatVM.confirmCheckInNo(messageId: message.id) }
                                    )
                                }
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChangeCompat(of: chatVM.messages.last?.content) { _ in
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

            // Preview de las imágenes antes de enviar
            if !chatVM.pendingImagesBase64.isEmpty {
                PendingImagesPreviewView(
                    imagesBase64: chatVM.pendingImagesBase64,
                    maxImages: Int(ChatViewModelKt.MAX_PENDING_IMAGES),
                    onRemove: { index in chatVM.removePendingImage(index: index) }
                )
            }

            // Input
            ChatInputBarView(
                text: $inputText,
                isLoading: chatVM.isLoading,
                hasPendingImage: !chatVM.pendingImagesBase64.isEmpty,
                onCameraClick: { imageSource = .camera },
                onGalleryClick: { imageSource = .gallery },
                onSend: {
                    let text = inputText.trimmingCharacters(in: .whitespaces)
                    guard !text.isEmpty || !chatVM.pendingImagesBase64.isEmpty else { return }
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
            switch source {
            case .camera:
                // Una toma a la vez; se acumula al adjuntar (attachImage agrega a la lista).
                ImagePicker(sourceType: .camera) { image in
                    if let image = image, let base64 = image.resizeAndEncode(maxSize: 800) {
                        chatVM.attachImage(base64: base64)
                    }
                }
            case .gallery:
                MultiImagePicker(selectionLimit: Int(ChatViewModelKt.MAX_PENDING_IMAGES)) { images in
                    for image in images {
                        if let base64 = image.resizeAndEncode(maxSize: 800) {
                            chatVM.attachImage(base64: base64)
                        }
                    }
                }
            }
        }
    }
}
