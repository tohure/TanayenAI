//
// Created by Carlo Huaman Torres on 8/04/26.
//

import SwiftUI
import Shared
import KMPNativeCoroutinesAsync
import PDFKit

struct ClinicalFieldEntry: Identifiable {
    let id = UUID()
    let name: String
    let unit: String
    let kmpField: ClinicalField
}

let allClinicalFields: [ClinicalFieldEntry] = [
    .init(name: "Glucosa en ayunas", unit: "mg/dL", kmpField: .fastingGlucose),
    .init(name: "Presión sistólica", unit: "mmHg", kmpField: .systolicPressure),
    .init(name: "Presión diastólica", unit: "mmHg", kmpField: .diastolicPressure),
    .init(name: "Colesterol total", unit: "mg/dL", kmpField: .cholesterolTotal),
    .init(name: "HDL", unit: "mg/dL", kmpField: .hdl),
    .init(name: "LDL", unit: "mg/dL", kmpField: .ldl),
    .init(name: "Triglicéridos", unit: "mg/dL", kmpField: .triglycerides),
    .init(name: "HbA1c", unit: "%", kmpField: .hba1c),
    .init(name: "TSH", unit: "µU/mL", kmpField: .tsh),
    .init(name: "Vitamina D", unit: "ng/mL", kmpField: .vitaminD),
    .init(name: "Ácido úrico", unit: "mg/dL", kmpField: .uricAcid),
    .init(name: "Ferritina", unit: "ng/mL", kmpField: .ferritin),
    .init(name: "Hemoglobina", unit: "g/dL", kmpField: .hemoglobin),
    .init(name: "PCR ultrasensible", unit: "mg/L", kmpField: .crp),
    .init(name: "Creatinina", unit: "mg/dL", kmpField: .creatinine)
]

@MainActor
class ClinicalProfileViewModelWrapper: ObservableObject {
    @Published var isLoading = true
    @Published var isExtracting = false
    @Published var extractionSuccess = false
    @Published var extractedValuesCount = 0
    @Published var error: String?
    @Published var profile: ClinicalProfile_?
    @Published var showDocumentPicker = false

    private let viewModel: ClinicalProfileViewModel
    private let pdfPicker: PdfPicker
    private var observeTask: Task<Void, Never>?

    init() {
        let userId = ConstantsKt.PROTOTYPE_USER_ID
        self.viewModel = KoinInitializerKt.getClinicalProfileViewModel(userId: userId)
        self.pdfPicker = KoinInitializerKt.getPdfPicker()

        PdfPickerBridge.shared.onPickerRequested {
            DispatchQueue.main.async { [weak self] in
                self?.showDocumentPicker = true
            }
        }

        observeTask = Task {
            do {
                for try await state in asyncSequence(for: viewModel.uiStateFlow) {
                    updateFromState(state)
                }
            } catch {
                print("ClinicalProfileViewModel state error: \(error)")
            }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    func pickAndExtractPdf() {
        viewModel.pickAndExtractPdf()
    }

    func saveIndividualValue(field: ClinicalField, value: Float) {
        viewModel.saveIndividualValue(field: field, value: value)
    }

    func clearError() { viewModel.clearError() }
    func clearSuccess() { viewModel.clearSuccess() }

    func handlePickedPDF(url: URL) {
        Task {
            let accessed = url.startAccessingSecurityScopedResource()
            defer { if accessed { url.stopAccessingSecurityScopedResource() } }

            let images = await Task.detached(priority: .userInitiated) {
                guard let data = try? Data(contentsOf: url) else { return [String]() }
                return renderPdfToJpegPages(data: data)
            }.value

            if images.isEmpty {
                pdfPicker.deliverError(message: "No se pudo renderizar el PDF")
            } else {
                pdfPicker.deliverPageImages(images: images)
            }
        }
    }

    private func updateFromState(_ state: ClinicalProfileUiState) {
        self.isLoading = state.isLoading
        self.isExtracting = state.isExtracting
        self.extractionSuccess = state.extractionSuccess
        self.extractedValuesCount = Int(state.extractedValuesCount)
        self.error = state.error
        self.profile = state.profile
    }
}

private func renderPdfToJpegPages(data: Data) -> [String] {
    guard let document = PDFDocument(data: data) else { return [] }
    var images: [String] = []
    let pageCount = min(document.pageCount, 5)
    for pageIndex in 0..<pageCount {
        guard let page = document.page(at: pageIndex) else { continue }
        let bounds = page.bounds(for: .mediaBox)
        let targetWidth: CGFloat = 1080
        let scale = targetWidth / bounds.width
        let size = CGSize(width: targetWidth, height: bounds.height * scale)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            UIColor.white.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))
            ctx.cgContext.translateBy(x: 0, y: size.height)
            ctx.cgContext.scaleBy(x: scale, y: -scale)
            page.draw(with: .mediaBox, to: ctx.cgContext)
        }
        if let jpeg = image.jpegData(compressionQuality: 0.9) {
            images.append(jpeg.base64EncodedString())
        }
    }
    return images
}
