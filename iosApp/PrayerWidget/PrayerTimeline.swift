//
//  PrayerTimeline.swift
//  PrayerWidget
//

import WidgetKit
import SwiftUI

struct PrayerEntry: TimelineEntry {
    let date: Date
    let snapshot: PrayerSnapshot?
}

/// One entry per prayer instant, not a periodic refresh.
///
/// WidgetKit will not wake the app on a schedule, so the widget has to be correct on its own for as
/// long as the snapshot reaches (three days). Cutting the timeline at each prayer time is what moves
/// the highlight forward; the live countdown between those points is `Text(timerInterval:)`, which
/// ticks in the host process without any reload at all.
struct PrayerTimelineProvider: TimelineProvider {

    /// A timeline is capped by the system anyway; three days of six times is comfortably inside it.
    private let maxEntries = 40

    func placeholder(in context: Context) -> PrayerEntry {
        PrayerEntry(date: Date(), snapshot: PrayerSnapshotStore.placeholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        let snapshot = context.isPreview
            ? PrayerSnapshotStore.placeholder
            : (PrayerSnapshotStore.load() ?? PrayerSnapshotStore.placeholder)

        completion(PrayerEntry(date: Date(), snapshot: snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        let snapshot = PrayerSnapshotStore.load()

        guard let snapshot, snapshot.hasTimes else {
            // No location yet (or the app has never run): one entry, retried in an hour.
            let entry = PrayerEntry(date: now, snapshot: snapshot)
            completion(Timeline(entries: [entry], policy: .after(now.addingTimeInterval(3600))))
            return
        }

        var dates = [now]
        dates += snapshot.allItems
            .map(\.date)
            .filter { $0 > now }
            .prefix(maxEntries - 1)

        let entries = dates.map { PrayerEntry(date: $0, snapshot: snapshot) }

        // `.atEnd` rather than a fixed interval: the app rewrites the snapshot every time it comes
        // to the foreground or reschedules notifications, so a periodic wake would mostly be waste.
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}
