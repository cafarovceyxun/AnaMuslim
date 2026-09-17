import SwiftUI
import Combine
import shared

/// Observes Compose-published system-chrome state (see `IosSystemChrome`) so SwiftUI can apply it:
/// the reader's fullscreen mode, which hides the status bar, and the share-image editor's portrait
/// lock, which narrows the app delegate's orientation mask.
final class SystemChromeModel: ObservableObject {
    @Published var statusBarHidden: Bool = false

    init() {
        statusBarHidden = IosSystemChrome.shared.statusBarHidden
        applyOrientationLock(IosSystemChrome.shared.portraitLocked)
        IosSystemChrome.shared.listener = { [weak self] in
            // Called from Compose on an arbitrary thread; hop to main for SwiftUI.
            DispatchQueue.main.async {
                self?.statusBarHidden = IosSystemChrome.shared.statusBarHidden
                self?.applyOrientationLock(IosSystemChrome.shared.portraitLocked)
            }
        }
    }

    /// Changing `AppDelegate.orientationLock` alone only takes effect the next time iOS asks, which
    /// for an already-rotated device is never. So the scene is asked to rotate (`requestGeometryUpdate`)
    /// and the root controller is told its supported set changed — the pair is what actually moves a
    /// landscape device back upright, and what lets go of it again on the way out.
    private func applyOrientationLock(_ locked: Bool) {
        let mask: UIInterfaceOrientationMask = locked ? .portrait : AppDelegate.defaultOrientations
        AppDelegate.orientationLock = mask

        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        for scene in scenes {
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask))
            scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        }
    }
}

struct ContentView: View {
    @StateObject private var chrome = SystemChromeModel()

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose handles safe area
            .statusBarHidden(chrome.statusBarHidden)
            // Dərin link (anamuslim://dua/<id>, anamuslim://asma/<no>). Analiz və naviqasiya
            // paylaşılan koddadır — burada yalnız URL ötürülür, yəni Android ilə eyni qaydalar
            // işləyir. Compose hələ qurulmayıbsa da problem yoxdur: link `DuaDeepLinkRouter`-də
            // gözləyir və kökdəki host onu hazır olanda oxuyur.
            .onOpenURL { url in
                _ = DuaDeepLinkRouter.shared.open(url: url.absoluteString)
            }
    }
}
