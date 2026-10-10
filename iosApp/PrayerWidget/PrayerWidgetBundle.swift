//
//  PrayerWidgetBundle.swift
//  PrayerWidget
//
//  The prayer widgets mirror the two Android ones (the next prayer alone, and the whole day); the
//  recitation player is the counterpart of Android's player widget (`PlayerWidget.swift`).
//

import SwiftUI
import WidgetKit

@main
struct PrayerWidgetBundle: WidgetBundle {
    var body: some Widget {
        PrayerNextWidget()
        PrayerTimesWidget()
        PlayerWidget()
    }
}

struct PrayerNextWidget: Widget {
    let kind = "PrayerNextWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PrayerTimelineProvider()) { entry in
            PrayerNextView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringKey("widget.next.title"))
        .description(LocalizedStringKey("widget.next.description"))
        .supportedFamilies([.systemSmall])
    }
}

struct PrayerTimesWidget: Widget {
    let kind = "PrayerTimesWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PrayerTimelineProvider()) { entry in
            PrayerTimesView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringKey("widget.times.title"))
        .description(LocalizedStringKey("widget.times.description"))
        .supportedFamilies([.systemMedium])
    }
}
