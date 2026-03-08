//
// Created by Carlo Huaman Torres on 6/03/26.
//

import SwiftUI

struct TanayenTheme {
    static let background = Color(hex: "#F7F6F3")
    static let surface = Color.white
    static let primaryGreen = Color(hex: "#2D6A4F")
    static let secondaryMint = Color(hex: "#74C69D")
    static let accentTerra = Color(hex: "#F4A261")
    static let textDark = Color(hex: "#1A1A2E")
    static let textMuted = Color(hex: "#8D8D9B")
    static let errorRed = Color(hex: "#E63946")
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let red = Double((int >> 16) & 0xFF) / 255.0
        let green = Double((int >> 8) & 0xFF) / 255.0
        let blue = Double(int & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}
