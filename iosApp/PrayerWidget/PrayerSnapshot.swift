//
//  PrayerSnapshot.swift
//  PrayerWidget
//
//  The widget renders content the app already prepared. Nothing here computes prayer times or
//  translates anything: the shared Kotlin layer writes a finished snapshot into the App Group
//  (`PrayerWidgetSnapshot.kt`), so labels arrive in the app's language rather than the device's,
//  and the extension stays free of the shared framework.
//

import Foundation

struct PrayerSnapshot: Codable {
    struct Item: Codable, Identifiable {
        let label: String
        let clock: String
        let atMillis: Int64
        let icon: String
        /// Sunrise is not a prayer, and the user can switch individual times off — only these
        /// take part in "which one is next".
        let countsAsNext: Bool

        var id: Int64 { atMillis }
        var date: Date { Date(timeIntervalSince1970: Double(atMillis) / 1000.0) }
    }

    struct Day: Codable {
        let dateIso: String
        let dateLine: String
        let items: [Item]
    }

    let generatedAtMillis: Int64
    let placeName: String
    let title: String
    let noLocationLabel: String
    let remainingLabel: String
    let days: [Day]

    var hasTimes: Bool { days.contains { !$0.items.isEmpty } }

    /// Every time across every day, ordered — the timeline and the "next" lookup both walk this.
    var allItems: [Item] { days.flatMap(\.items).sorted { $0.atMillis < $1.atMillis } }

    /// The day whose times are shown at `date`; falls back to the first day the snapshot carries.
    func day(containing date: Date) -> Day? {
        days.last { day in
            guard let first = day.items.first else { return false }
            return first.date <= date
        } ?? days.first
    }

    func next(after date: Date) -> Item? {
        allItems.first { $0.countsAsNext && $0.date > date }
    }
}

enum PrayerSnapshotStore {
    /// Must match `IosPrayerWidgetBridge.APP_GROUP_ID` and both entitlements files.
    static let appGroupId = "group.com.cafarovceyxun.anamuslim"
    static let snapshotKey = "prayer_widget_snapshot"

    static func load() -> PrayerSnapshot? {
        guard
            let defaults = UserDefaults(suiteName: appGroupId),
            let raw = defaults.string(forKey: snapshotKey),
            let data = raw.data(using: .utf8)
        else { return nil }

        return try? JSONDecoder().decode(PrayerSnapshot.self, from: data)
    }

    /// Shown in the widget gallery and before the app has ever run.
    static var placeholder: PrayerSnapshot {
        let start = Date().timeIntervalSince1970 * 1000
        let names = ["Fəcr", "Günəş", "Zöhr", "Əsr", "Axşam", "İşa"]
        let clocks = ["05:29", "06:30", "12:55", "16:31", "19:20", "20:20"]
        let icons = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

        let items = (0..<6).map { index in
            PrayerSnapshot.Item(
                label: names[index],
                clock: clocks[index],
                atMillis: Int64(start) + Int64(index + 1) * 3_600_000,
                icon: icons[index],
                countsAsNext: icons[index] != "sunrise"
            )
        }

        return PrayerSnapshot(
            generatedAtMillis: Int64(start),
            placeName: "",
            title: "Namaz vaxtları",
            noLocationLabel: "Yer seçin",
            remainingLabel: "qaldı",
            days: [PrayerSnapshot.Day(dateIso: "", dateLine: "", items: items)]
        )
    }
}
