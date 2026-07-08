import SwiftUI
import UIKit
import PhotosUI

/// Selección múltiple desde la galería (hasta `selectionLimit` fotos en un solo envío).
/// La cámara sigue usando `ImagePicker` (una toma a la vez, acumulativa).
struct MultiImagePicker: UIViewControllerRepresentable {
    @Environment(\.presentationMode) var presentationMode
    var selectionLimit: Int
    var onImagesPicked: ([UIImage]) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = selectionLimit
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: MultiImagePicker

        init(_ parent: MultiImagePicker) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            parent.presentationMode.wrappedValue.dismiss()
            guard !results.isEmpty else {
                parent.onImagesPicked([])
                return
            }

            var images: [UIImage] = []
            let group = DispatchGroup()

            for result in results {
                let provider = result.itemProvider
                guard provider.canLoadObject(ofClass: UIImage.self) else { continue }
                group.enter()
                provider.loadObject(ofClass: UIImage.self) { object, _ in
                    if let image = object as? UIImage {
                        images.append(image)
                    }
                    group.leave()
                }
            }

            group.notify(queue: .main) {
                self.parent.onImagesPicked(images)
            }
        }
    }
}

struct ImagePicker: UIViewControllerRepresentable {
    @Environment(\.presentationMode) var presentationMode
    var sourceType: UIImagePickerController.SourceType
    var onImagePicked: (UIImage?) -> Void

    func makeUIViewController(context: UIViewControllerRepresentableContext<ImagePicker>) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = sourceType
        return picker
    }

    func updateUIViewController(
        _ uiViewController: UIImagePickerController,
        context: UIViewControllerRepresentableContext<ImagePicker>
    ) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let uiImage = info[.originalImage] as? UIImage {
                parent.onImagePicked(uiImage)
            } else {
                parent.onImagePicked(nil)
            }
            parent.presentationMode.wrappedValue.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onImagePicked(nil)
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}

extension UIImage {
    func resizeAndEncode(maxSize: CGFloat = 800) -> String? {
        let size = self.size
        let ratio: CGFloat = {
            if size.width > maxSize || size.height > maxSize {
                return min(maxSize / size.width, maxSize / size.height)
            }
            return 1.0
        }()

        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        let rect = CGRect(origin: .zero, size: newSize)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        self.draw(in: rect)
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        guard let finalImage = newImage, let data = finalImage.jpegData(compressionQuality: 0.7) else {
            return nil
        }

        return data.base64EncodedString(options: .lineLength64Characters)
    }
}
