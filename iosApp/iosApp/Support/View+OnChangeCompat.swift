import SwiftUI

extension View {
    /// `onChange` compatible con iOS 16 y iOS 17+.
    ///
    /// En iOS 17+ usa la firma de dos parámetros `(oldValue, newValue)`; en iOS 16 usa
    /// la de un parámetro (deprecada en iOS 17). Así se mantiene el soporte para iOS 16
    /// sin generar warnings de deprecación en compilaciones para iOS 17+.
    ///
    /// El closure recibe el **nuevo** valor.
    @ViewBuilder
    func onChangeCompat<Value: Equatable>(
        of value: Value,
        perform action: @escaping (Value) -> Void
    ) -> some View {
        if #available(iOS 17.0, *) {
            onChange(of: value) { _, newValue in action(newValue) }
        } else {
            onChange(of: value) { newValue in action(newValue) }
        }
    }
}
