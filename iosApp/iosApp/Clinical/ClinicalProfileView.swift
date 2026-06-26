//
// Created by Carlo Huaman Torres on 8/04/26.
//

import SwiftUI
import Shared
import UniformTypeIdentifiers

struct ClinicalProfileView: View {
    @StateObject private var wrapper = ClinicalProfileViewModelWrapper()
    @State private var selectedField: ClinicalFieldEntry?
    @State private var manualValueText = ""
    @State private var showGeminiToken = false
    @State private var easterTapCount = 0
    @State private var easterLastTap = Date.distantPast

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {

                // ── Título ────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("Perfil Clínico")
                        .font(.system(size: 22, weight: .semibold, design: .rounded))
                        .foregroundColor(TanayenTheme.textDark)
                    Text("Sube tu PDF de laboratorio, toma foto del análisis desde el chat, " +
                         "escríbele tus valores a Tanayen, o ingrésalos manualmente.")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.textMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 24)
                .padding(.top, 16)

                // ── PDF ───────────────────────────────────────────────────
                CardView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("📄 Subir análisis (PDF)")
                            .font(.system(.headline, design: .rounded, weight: .semibold))
                            .foregroundColor(TanayenTheme.textDark)
                        Text("Gemini extrae todos los valores automáticamente.")
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)

                        if wrapper.isExtracting {
                            HStack(spacing: 10) {
                                ProgressView()
                                    .tint(TanayenTheme.primaryGreen)
                                Text("Analizando PDF...")
                                    .font(.system(.subheadline, design: .rounded))
                                    .foregroundColor(TanayenTheme.textMuted)
                            }
                        } else {
                            Button(
                                action: { wrapper.pickAndExtractPdf() },
                                label: {
                                    Text("Seleccionar PDF")
                                        .font(.system(.headline, design: .rounded, weight: .semibold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 48)
                                        .background(TanayenTheme.primaryGreen)
                                        .cornerRadius(12)
                                }
                            )
                        }

                        if wrapper.extractionSuccess {
                            Text("✓ \(wrapper.extractedValuesCount) valores extraídos")
                                .font(.system(.subheadline, design: .rounded, weight: .semibold))
                                .foregroundColor(TanayenTheme.secondaryMint)
                        }
                    }
                }

                // ── Info: foto o chat ─────────────────────────────────────
                CardView(background: Color(hex: "#E8F5EE"), elevated: false) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("📷 Foto o 💬 Chat")
                            .font(.system(.headline, design: .rounded, weight: .semibold))
                            .foregroundColor(TanayenTheme.primaryGreen)
                        Text("También puedes tomar una foto de tu análisis impreso desde el chat, " +
                             "o escribirle a Tanayen: \"mi glucosa hoy fue 98\". " +
                             "Te preguntará si quieres guardarlo.")
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.primaryGreen)
                    }
                }

                // ── Formulario manual ─────────────────────────────────────
                CardView {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("✏️ Agregar valor individual")
                            .font(.system(.headline, design: .rounded, weight: .semibold))
                            .foregroundColor(TanayenTheme.textDark)
                        Text("Para valores de glucómetro, tensiómetro u otros dispositivos.")
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)

                        let columns = [GridItem(.flexible()), GridItem(.flexible())]
                        LazyVGrid(columns: columns, spacing: 8) {
                            ForEach(allClinicalFields) { entry in
                                Button(
                                    action: {
                                        manualValueText = ""
                                        selectedField = entry
                                    },
                                    label: {
                                        Text(entry.name)
                                            .font(.system(.caption, design: .rounded))
                                            .foregroundColor(TanayenTheme.primaryGreen)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(TanayenTheme.primaryGreen, lineWidth: 1)
                                            )
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Restricciones activas ─────────────────────────────────
                if let profile = wrapper.profile,
                   let restrictions = profile.activeRestrictions as? [String],
                   !restrictions.isEmpty {
                    CardView(background: Color(hex: "#FFF3E0"), elevated: false) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Restricciones activas")
                                .font(.system(.headline, design: .rounded, weight: .semibold))
                                .foregroundColor(TanayenTheme.accentTerra)
                            ForEach(restrictions, id: \.self) { restriction in
                                Text("• \(restriction)")
                                    .font(.system(.subheadline, design: .rounded))
                                    .foregroundColor(TanayenTheme.accentTerra)
                            }
                        }
                    }
                }

                // ── Grupos de valores clínicos ────────────────────────────
                if let profile = wrapper.profile {
                    ClinicalGroupCard(title: "Perfil lipídico", values: [
                        ("Colesterol total", profile.cholesterolTotal.map { "\(Int($0.floatValue)) mg/dL" }),
                        ("HDL", profile.hdl.map { "\(Int($0.floatValue)) mg/dL" }),
                        ("LDL", profile.ldl.map { "\(Int($0.floatValue)) mg/dL" }),
                        ("Triglicéridos", profile.triglycerides.map { "\(Int($0.floatValue)) mg/dL" })
                    ])
                    ClinicalGroupCard(title: "Glucosa", values: [
                        ("Glucosa en ayunas", profile.fastingGlucose.map { "\(Int($0.floatValue)) mg/dL" }),
                        ("HbA1c", profile.hba1c.map { "\($0.floatValue) %" }),
                        ("Insulina en ayunas", profile.fastingInsulin.map { "\($0.floatValue) µU/mL" }),
                        ("HOMA-IR", profile.homaIr.map { "\($0.floatValue)" })
                    ])
                    ClinicalGroupCard(title: "Función renal", values: [
                        ("Creatinina", profile.creatinine.map { "\($0.floatValue) mg/dL" }),
                        ("Urea", profile.urea.map { "\(Int($0.floatValue)) mg/dL" }),
                        ("FG estimado", profile.gfr.map { "\(Int($0.floatValue)) mL/min" }),
                        ("Ácido úrico", profile.uricAcid.map { "\($0.floatValue) mg/dL" })
                    ])
                    ClinicalGroupCard(title: "Función hepática", values: [
                        ("ALT/TGP", profile.alt.map { "\(Int($0.floatValue)) U/L" }),
                        ("AST/TGO", profile.ast.map { "\(Int($0.floatValue)) U/L" }),
                        ("GGT", profile.ggt.map { "\(Int($0.floatValue)) U/L" })
                    ])
                    ClinicalGroupCard(title: "Tiroides", values: [
                        ("TSH", profile.tsh.map { "\($0.floatValue) µU/mL" }),
                        ("T3 libre", profile.t3Free.map { "\($0.floatValue) pg/mL" }),
                        ("T4 libre", profile.t4Free.map { "\($0.floatValue) ng/dL" })
                    ])
                    ClinicalGroupCard(title: "Hemograma", values: [
                        ("Hemoglobina", profile.hemoglobin.map { "\($0.floatValue) g/dL" }),
                        ("Hematocrito", profile.hematocrit.map { "\($0.floatValue) %" }),
                        ("Ferritina", profile.ferritin.map { "\(Int($0.floatValue)) ng/mL" }),
                        ("Hierro sérico", profile.serumIron.map { "\(Int($0.floatValue)) µg/dL" })
                    ])
                    ClinicalGroupCard(title: "Vitaminas y minerales", values: [
                        ("Vitamina D", profile.vitaminD.map { "\(Int($0.floatValue)) ng/mL" }),
                        ("Vitamina B12", profile.vitaminB12.map { "\(Int($0.floatValue)) pg/mL" }),
                        ("Folato", profile.folate.map { "\($0.floatValue) ng/mL" }),
                        ("Zinc", profile.zinc.map { "\(Int($0.floatValue)) µg/dL" }),
                        ("Magnesio", profile.magnesium.map { "\($0.floatValue) mg/dL" })
                    ])
                    ClinicalGroupCard(title: "Inflamación", values: [
                        ("PCR ultrasensible", profile.crpUltraSensitive.map { "\($0.floatValue) mg/L" }),
                        ("Homocisteína", profile.homocysteine.map { "\($0.floatValue) µmol/L" })
                    ])
                }

                // ── Error ─────────────────────────────────────────────────
                if let error = wrapper.error {
                    Text(error)
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundColor(TanayenTheme.errorRed)
                        .padding(.horizontal, 24)
                        .onAppear { DispatchQueue.main.asyncAfter(deadline: .now() + 4) { wrapper.clearError() } }
                }

                // ── Easter egg ────────────────────────────────────────────
                Text("🌿")
                    .font(.system(size: 18))
                    .padding(.top, 32)
                    .onTapGesture {
                        let now = Date()
                        if now.timeIntervalSince(easterLastTap) < 0.6 {
                            easterTapCount += 1
                        } else {
                            easterTapCount = 1
                        }
                        easterLastTap = now
                        if easterTapCount >= 5 {
                            easterTapCount = 0
                            showGeminiToken = true
                        }
                    }

                Spacer(minLength: 24)
            }
        }
        .background(TanayenTheme.background)
        .navigationBarHidden(true)
        // ── Gemini Token (easter egg) ─────────────────────────────────────
        .sheet(isPresented: $showGeminiToken) {
            GeminiTokenView()
        }
        // ── Document picker ───────────────────────────────────────────────
        .sheet(isPresented: $wrapper.showDocumentPicker) {
            DocumentPickerView { url in
                wrapper.showDocumentPicker = false
                wrapper.handlePickedPDF(url: url)
            } onCancel: {
                wrapper.showDocumentPicker = false
            }
        }
        // ── Manual entry sheet ────────────────────────────────────────────
        .sheet(item: $selectedField) { entry in
            ManualEntrySheet(entry: entry, onSave: { value in
                wrapper.saveIndividualValue(field: entry.kmpField, value: value)
                selectedField = nil
            }, onCancel: {
                selectedField = nil
            })
        }
    }
}

// ── Subcomponentes ─────────────────────────────────────────────────────────────

private struct CardView<Content: View>: View {
    var background: Color = .white
    var elevated: Bool = true
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background)
            .cornerRadius(16)
            .shadow(color: elevated ? Color.black.opacity(0.06) : .clear, radius: 4, x: 0, y: 2)
            .padding(.horizontal, 24)
    }
}

private struct ClinicalGroupCard: View {
    let title: String
    let values: [(String, String?)]

    var body: some View {
        let nonNull = values.filter { $0.1 != nil }
        if nonNull.isEmpty { return AnyView(EmptyView()) }
        return AnyView(
            CardView {
                VStack(alignment: .leading, spacing: 8) {
                    Text(title)
                        .font(.system(.headline, design: .rounded, weight: .semibold))
                        .foregroundColor(TanayenTheme.textDark)
                    ForEach(nonNull, id: \.0) { name, value in
                        HStack {
                            Text(name)
                                .font(.system(.subheadline, design: .rounded))
                                .foregroundColor(TanayenTheme.textMuted)
                            Spacer()
                            Text(value ?? "")
                                .font(.system(.subheadline, design: .rounded, weight: .semibold))
                                .foregroundColor(TanayenTheme.textDark)
                        }
                    }
                }
            }
        )
    }
}

private struct ManualEntrySheet: View {
    let entry: ClinicalFieldEntry
    let onSave: (Float) -> Void
    let onCancel: () -> Void

    @State private var text = ""

    var parsedValue: Float? { Float(text.replacingOccurrences(of: ",", with: ".")) }

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                ZStack {
                    if text.isEmpty {
                        Text("0.0")
                            .font(.system(size: 32, weight: .light, design: .rounded))
                            .foregroundColor(TanayenTheme.textMuted)
                            .allowsHitTesting(false)
                    }
                    TextField("", text: $text)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 32, weight: .light, design: .rounded))
                        .foregroundColor(TanayenTheme.textDark)
                        .multilineTextAlignment(.center)
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(TanayenTheme.surface)
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#E0E0E0"), lineWidth: 1))
                .padding(.horizontal, 32)
                .padding(.top, 32)

                Text(entry.unit)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)

                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(TanayenTheme.background)
            .navigationTitle(entry.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { onCancel() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        if let value = parsedValue { onSave(value) }
                    }
                    .disabled(parsedValue == nil)
                    .tint(TanayenTheme.primaryGreen)
                }
            }
        }
    }
}

private struct DocumentPickerView: UIViewControllerRepresentable {
    let onPicked: (URL) -> Void
    let onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.pdf])
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPicked: onPicked, onCancel: onCancel) }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onPicked: (URL) -> Void
        let onCancel: () -> Void
        init(onPicked: @escaping (URL) -> Void, onCancel: @escaping () -> Void) {
            self.onPicked = onPicked; self.onCancel = onCancel
        }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            onPicked(url)
        }
        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            onCancel()
        }
    }
}
