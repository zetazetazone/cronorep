import SwiftUI

@main
struct CronoRepApp: App {
    @StateObject private var timer = RestTimer()
    @StateObject private var store = Store()
    @StateObject private var look = Look()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(timer)
                .environmentObject(store)
                .environmentObject(look)
        }
    }
}

func formatTime(_ totalSeconds: Int) -> String {
    String(format: "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

extension Color {
    static let restGreen = Color(red: 0.29, green: 0.87, blue: 0.50)
    static let goOrange = Color(red: 0.98, green: 0.57, blue: 0.24)
    static let dim = Color(red: 0.61, green: 0.64, blue: 0.69)
}
