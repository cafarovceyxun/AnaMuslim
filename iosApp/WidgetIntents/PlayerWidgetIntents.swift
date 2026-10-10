//
//  PlayerWidgetIntents.swift
//  Compiled into BOTH the app and the widget extension (the `WidgetIntents` synchronized group is a
//  member of both targets).
//
//  The widget needs the type to build `Button(intent:)`; the app needs it because an
//  `AudioPlaybackIntent` is performed **in the app's process** — iOS launches the app in the
//  background if it is not running, which is the only place the Kotlin player lives. The extension
//  does not link the shared framework, so `perform()` reaches the player through
//  `PlayerWidgetIntentRouter`, a closure only the app installs (`AppDelegate`).
//

import AppIntents

/// The commands the widget sends. Raw values must match the `ACTION_*` constants in
/// `IosPlayerWidgetBridge.kt`.
enum PlayerWidgetAction: String {
    case toggle
    case nextVerse
    case previousVerse
    case nextChapter
    case previousChapter
}

enum PlayerWidgetIntentRouter {
    /// Installed by the app at launch. Stays `nil` inside the extension — which never performs the
    /// intent itself, because `AudioPlaybackIntent` always runs in the app.
    @MainActor static var handler: ((PlayerWidgetAction) async -> Void)?
}

struct PlayerWidgetControlIntent: AudioPlaybackIntent {
    static var title: LocalizedStringResource = "Recitation player control"

    /// Not a Shortcuts action — it only makes sense behind a widget button.
    static var isDiscoverable: Bool = false

    @Parameter(title: "Action")
    var action: String

    init() {}

    init(_ action: PlayerWidgetAction) {
        self.action = action.rawValue
    }

    /// `@MainActor`: Kotlin/Native refuses to start a suspend function off the main thread.
    @MainActor
    func perform() async throws -> some IntentResult {
        if let action = PlayerWidgetAction(rawValue: action) {
            await PlayerWidgetIntentRouter.handler?(action)
        }
        return .result()
    }
}
