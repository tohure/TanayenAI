import SwiftUI

struct NameInputView: View {
    @State private var name: String
    let onSave: (String) -> Void
    let onSkip: () -> Void

    init(initialName: String, onSave: @escaping (String) -> Void, onSkip: @escaping () -> Void) {
        _name = State(initialValue: initialName)
        self.onSave = onSave
        self.onSkip = onSkip
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("¿Cómo te llamas?")
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .padding(.top, 8)

            Text("Así te saludaré cada vez que abras la app.")
                .font(.system(size: 14, weight: .regular, design: .rounded))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            TextField("Tu nombre", text: $name)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()

            HStack(spacing: 16) {
                Button("Saltar") {
                    onSkip()
                }
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.secondary.opacity(0.12))
                .cornerRadius(12)

                Button("Guardar") {
                    onSave(name)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "#2D6A4F"))
                .cornerRadius(12)
            }
        }
        .padding(28)
    }
}
