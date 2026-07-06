import SwiftUI
import UserNotifications

struct NotificationSettingsView: View {

    @Environment(\.dismiss) private var dismiss
    @StateObject private var wrapper = NotificationSettingsViewModelWrapper()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {

                    // ── Morning advice card ────────────────────────────────
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Consejo matutino")
                                    .font(.system(.body, design: .rounded, weight: .semibold))
                                    .foregroundColor(TanayenTheme.textDark)
                                Text("Recibe un consejo nutricional cada mañana")
                                    .font(.system(.caption, design: .rounded))
                                    .foregroundColor(TanayenTheme.textMuted)
                            }
                            Spacer()
                            Toggle("", isOn: $wrapper.morningEnabled)
                                .tint(TanayenTheme.primaryGreen)
                                .onChangeCompat(of: wrapper.morningEnabled) { newValue in
                                    if newValue { requestPermission() }
                                }
                        }

                        if wrapper.morningEnabled {
                            Divider().opacity(0.3)

                            HStack {
                                Text("Hora del consejo")
                                    .font(.system(.subheadline, design: .rounded, weight: .semibold))
                                    .foregroundColor(TanayenTheme.textDark)
                                Spacer()
                                DatePicker(
                                    "",
                                    selection: Binding(
                                        get: {
                                            Calendar.current.date(
                                                bySettingHour: wrapper.hour,
                                                minute: wrapper.minute,
                                                second: 0,
                                                of: Date()
                                            ) ?? Date()
                                        },
                                        set: { date in
                                            let comps = Calendar.current.dateComponents(
                                                [.hour, .minute], from: date
                                            )
                                            wrapper.hour = comps.hour ?? wrapper.hour
                                            wrapper.minute = comps.minute ?? wrapper.minute
                                        }
                                    ),
                                    displayedComponents: .hourAndMinute
                                )
                                .datePickerStyle(.compact)
                                .labelsHidden()
                                .tint(TanayenTheme.primaryGreen)
                                .colorScheme(.light)
                            }
                        }
                    }
                    .padding(20)
                    .background(TanayenTheme.surface)
                    .cornerRadius(16)

                    Button {
                        wrapper.save()
                        dismiss()
                    } label: {
                        Text("Guardar")
                            .font(.system(.body, design: .rounded, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(TanayenTheme.primaryGreen)
                            .cornerRadius(12)
                    }

                    Spacer()
                }
                .padding(20)
            }
            .background(TanayenTheme.background)
            .navigationTitle("Notificaciones")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") { dismiss() }
                        .foregroundColor(TanayenTheme.primaryGreen)
                }
            }
        }
    }

    private func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }
}
