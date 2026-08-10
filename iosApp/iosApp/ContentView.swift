import SwiftUI
import Combine
import shared

/// Observes Compose-published system-chrome state (see `IosSystemChrome`) so SwiftUI can apply it.
/// Currently just the reader's fullscreen mode, which hides the status bar.
final class SystemChromeModel: ObservableObject {
    @Published var statusBarHidden: Bool = false

    init() {
        statusBarHidden = IosSystemChrome.shared.statusBarHidden
        IosSystemChrome.shared.listener = { [weak self] in
            // Called from Compose on an arbitrary thread; hop to main for SwiftUI.
            DispatchQueue.main.async {
                self?.statusBarHidden = IosSystemChrome.shared.statusBarHidden
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var chrome = SystemChromeModel()

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose handles safe area
            .statusBarHidden(chrome.statusBarHidden)
    }
}
