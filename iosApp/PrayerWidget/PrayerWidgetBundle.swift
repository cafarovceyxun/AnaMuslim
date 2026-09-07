//
//  PrayerWidgetBundle.swift
//  PrayerWidget
//
//  Two widgets, mirroring the two Android ones: the next prayer alone, and the whole day.
//

import SwiftUI
import WidgetKit

@main
struct PrayerWidgetBundle: WidgetBundle {
    var body: some Widget {
        PrayerNextWidget()
        PrayerTimesWidget()
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
