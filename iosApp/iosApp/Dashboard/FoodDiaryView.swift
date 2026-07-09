//
//  FoodDiaryView.swift
//

import SwiftUI
import Shared

struct FoodDiaryView: View {
    @StateObject private var wrapper = FoodDiaryViewModelWrapper()
    @State private var pendingDelete: FoodDiaryEntryData?

    var body: some View {
        Group {
            if wrapper.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if wrapper.isEmpty {
                Text("Aún no hay comidas registradas 🍽️")
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(wrapper.days) { day in
                        Section {
                            ForEach(day.entries) { entry in
                                FoodDiaryEntryRow(entry: entry)
                                    .listRowBackground(TanayenTheme.surface)
                                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                        Button(role: .destructive) {
                                            pendingDelete = entry
                                        } label: {
                                            Label("Borrar", systemImage: "trash")
                                        }
                                    }
                            }
                        } header: {
                            HStack {
                                Text(day.label)
                                    .font(.system(.subheadline, design: .rounded, weight: .bold))
                                    .foregroundColor(TanayenTheme.textDark)
                                Spacer()
                                Text("\(day.totalCalories) kcal · \(day.mealCount) \(day.mealCount == 1 ? "comida" : "comidas")")
                                    .font(.system(.caption, design: .rounded))
                                    .foregroundColor(TanayenTheme.textMuted)
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
            }
        }
        .background(TanayenTheme.background)
        .navigationTitle("Diario de comidas")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            "¿Borrar este registro?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible,
            presenting: pendingDelete
        ) { entry in
            Button("Borrar", role: .destructive) {
                wrapper.deleteEntry(entry.id)
                pendingDelete = nil
            }
            Button("Cancelar", role: .cancel) { pendingDelete = nil }
        } message: { entry in
            Text("Se quitará \"\(entry.foodName)\" y se recalcularán las calorías del día.")
        }
    }
}

private struct FoodDiaryEntryRow: View {
    let entry: FoodDiaryEntryData

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.time)
                    .font(.system(.caption, design: .rounded, weight: .semibold))
                    .foregroundColor(TanayenTheme.textDark)
                Text(entry.mealType)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundColor(TanayenTheme.textMuted)
            }
            .frame(width: 64, alignment: .leading)

            Text(entry.foodName)
                .font(.system(.subheadline, design: .rounded))
                .foregroundColor(TanayenTheme.textDark)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text("\(entry.calories) kcal")
                .font(.system(.caption, design: .rounded, weight: .bold))
                .foregroundColor(TanayenTheme.primaryGreen)
        }
        .padding(.vertical, 4)
    }
}
