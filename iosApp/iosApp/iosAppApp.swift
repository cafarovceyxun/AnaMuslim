//
//  iosAppApp.swift
//  iosApp
//
//  Created by ceyhun on 13.07.26.
//

import SwiftUI
import UIKit
import shared

/// Bridges UIKit lifecycle events SwiftUI does not surface. Currently the Home-screen quick actions
/// ("continue reading", "verse of the day"), forwarded into the shared `IosQuickActions` registry,
/// which routes each tap to the reader by action type (the read side is shared Kotlin).
class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // BGTaskScheduler refuses handlers registered after launch finishes, so this cannot wait
        // for initSharedForIos() (which runs on the first composition — and on a background launch
        // never runs at all). The handler itself bootstraps the shared layer.
        IosBackgroundTasks.shared.register()

        // Cold launch via a quick action: the item is delivered here, not through performActionFor.
        // The Kotlin handler polls for the reader hook, so it is safe to forward this immediately.
        if let shortcut = launchOptions?[.shortcutItem] as? UIApplicationShortcutItem {
            _ = IosQuickActions.shared.handle(shortcutItem: shortcut)
            // Returning false tells iOS not to also call performActionFor for this same launch item.
            return false
        }
        return true
    }

    /// iOS relaunches the app in the background when a transfer from our background URLSession
    /// finishes. The handler must be kept and invoked once the session has replayed its events, or
    /// the system stops granting the app background time; `IosBackgroundDownloads` calls it from
    /// `URLSessionDidFinishEventsForBackgroundURLSession`.
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        IosBackgroundDownloads.shared.setBackgroundEventsCompletion(handler: completionHandler)
    }

    func application(
        _ application: UIApplication,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        // Warm activation (app already running in the background).
        let handled = IosQuickActions.shared.handle(shortcutItem: shortcutItem)
        completionHandler(handled)
    }
}

@main
struct iosAppApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
