//
//  PlayerSnapshot.swift
//  PrayerWidget
//
//  The recitation player widget renders what the app already prepared: the shared Kotlin layer
//  writes a finished snapshot into the App Group (`PlayerWidgetSnapshot.kt`,
//  `IosPlayerWidgetBridge.kt`), so surah names, the reciter and every label arrive in the app's
//  language and the extension never touches the Quran database.
//

import Foundation

struct PlayerSnapshot: Codable {
    struct Labels: Codable {
        let widgetTitle: String
        let play: String
        let pause: String
        let previousVerse: String
        let nextVerse: String
        let previousChapter: String
        let nextChapter: String
        /// Optional so a snapshot written by an older build still decodes.
        let verseText: String?
    }

    let generatedAtMillis: Int64
    let chapterNo: Int
    let verseNo: Int
    /// 0 while the Quran database is not ready — the progress bar is hidden then.
    let verseCount: Int
    /// The surah name as a `suracon` glyph, the same lettering the Android widget artwork uses.
    let chapterGlyph: String
    let title: String
    let verseLabel: String
    let reciter: String
    /// The verse being recited, uthmani with harakat, and its primary translation (large widget).
    /// Either can be empty before the database or translation is ready.
    let arabicText: String
    let translation: String
    let isPlaying: Bool
    let isLoading: Bool
    let labels: Labels

    var progress: Double {
        guard verseCount > 0 else { return 0 }
        return min(max(Double(verseNo) / Double(verseCount), 0), 1)
    }

    /// "Ayə 2 / 7" — the count is a bare number, so it needs no translation of its own.
    var progressLabel: String {
        verseCount > 0 ? "\(verseLabel) / \(verseCount)" : verseLabel
    }

    /// Opens the reader at the verse being recited (`IosPlayerWidgetBridge.openUrl`).
    var openURL: URL? {
        URL(string: "anamuslim://player?chapter=\(chapterNo)&verse=\(verseNo)")
    }
}

/// What the large widget shows of the verse being recited. The text button cycles it in the order
/// the user asked for: Arabic only → translation only → both (the default) — same as Android's
/// `VerseTextMode`.
enum PlayerVerseTextMode: Int {
    case both = 0
    case arabic = 1
    case translation = 2

    var showsArabic: Bool { self != .translation }
    var showsTranslation: Bool { self != .arabic }
    var next: PlayerVerseTextMode { PlayerVerseTextMode(rawValue: (rawValue + 1) % 3) ?? .both }

    /// Widget-only state, so it lives next to the snapshot but is written by the extension itself
    /// (`PlayerVerseTextModeIntent`) — the app never reads it.
    private static let key = "player_widget_verse_text_mode"

    static var current: PlayerVerseTextMode {
        let raw = UserDefaults(suiteName: PrayerSnapshotStore.appGroupId)?.integer(forKey: key) ?? 0
        return PlayerVerseTextMode(rawValue: raw) ?? .both
    }

    static func store(_ mode: PlayerVerseTextMode) {
        UserDefaults(suiteName: PrayerSnapshotStore.appGroupId)?.set(mode.rawValue, forKey: key)
    }
}

/// The large widget's verse text size in percent — A−/A+ step it (`PlayerVerseZoomIntent`). The
/// widget's stand-in for pinch-to-zoom, which WidgetKit cannot receive. Same range as Android's
/// `VerseZoom`.
enum PlayerVerseZoom {
    static let minPercent = 70
    static let maxPercent = 200
    static let stepPercent = 10
    private static let key = "player_widget_verse_zoom"

    static var current: Int {
        let stored = UserDefaults(suiteName: PrayerSnapshotStore.appGroupId)?.integer(forKey: key) ?? 0
        return stored == 0 ? 100 : min(max(stored, minPercent), maxPercent)
    }

    static func step(by delta: Int) {
        let next = min(max(current + delta * stepPercent, minPercent), maxPercent)
        UserDefaults(suiteName: PrayerSnapshotStore.appGroupId)?.set(next, forKey: key)
    }
}

enum PlayerSnapshotStore {
    /// Must match `IosPlayerWidgetBridge.SNAPSHOT_KEY`.
    static let snapshotKey = "player_widget_snapshot"

    static func load() -> PlayerSnapshot? {
        guard
            let defaults = UserDefaults(suiteName: PrayerSnapshotStore.appGroupId),
            let raw = defaults.string(forKey: snapshotKey),
            let data = raw.data(using: .utf8)
        else { return nil }

        return try? JSONDecoder().decode(PlayerSnapshot.self, from: data)
    }

    /// Widget gallery and the time before the app has ever run. The gallery title itself comes
    /// from `Localizable.strings`, which WidgetKit reads at build time.
    static var placeholder: PlayerSnapshot {
        PlayerSnapshot(
            generatedAtMillis: Int64(Date().timeIntervalSince1970 * 1000),
            chapterNo: 1,
            verseNo: 2,
            verseCount: 7,
            chapterGlyph: "\u{E904}",
            title: "Fatihə surəsi",
            verseLabel: "Ayə 2",
            reciter: "Mishary Rashid Alafasy",
            arabicText: "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
            translation: "Həmd olsun aləmlərin Rəbbi Allaha!",
            isPlaying: false,
            isLoading: false,
            labels: PlayerSnapshot.Labels(
                widgetTitle: "Qiraət pleyeri",
                play: "Oxut",
                pause: "Dayandır",
                previousVerse: "Əvvəlki ayə",
                nextVerse: "Növbəti ayə",
                previousChapter: "Əvvəlki surə",
                nextChapter: "Növbəti surə",
                verseText: "Mətnlər"
            )
        )
    }
}
