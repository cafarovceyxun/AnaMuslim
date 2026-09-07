//
//  PrayerWidgetViews.swift
//  PrayerWidget
//

import SwiftUI
import WidgetKit

enum PrayerWidgetStyle {
    /// Same green the Android widget uses: the brand colour lightened so it reads on a dark card.
    static let accent = Color(red: 0x19 / 255, green: 0xB3 / 255, blue: 0x7E / 255)

    /// SF Symbols instead of shipping artwork — the snapshot only carries the key.
    static func symbol(for icon: String) -> String {
        switch icon {
        case "fajr": return "sunrise"
        case "sunrise": return "sunrise.fill"
        case "dhuhr": return "sun.max.fill"
        case "asr": return "sun.min.fill"
        case "maghrib": return "sunset.fill"
        case "isha": return "moon.stars.fill"
        default: return "clock"
        }
    }
}

/// Widget background.
///
/// ⚠️ **Material QOYMA.** Əvvəlki cəhd altda `.ultraThinMaterial` işlədirdi; o, divar kağızını ağır
/// frost kimi desatürasiya edir və nəticə divar kağızı yox, **boz** görünür (istifadəçi tam bunu
/// gördü). Ana ekran vidcetinin arxasında sistemin öz lövhəsi yox, birbaşa divar kağızı durur, ona
/// görə həqiqi «şüşə» sadəcə **yarımşəffaf qara pərdə**dir: qatılıq aşağı düşdükcə divar kağızı olduğu
/// kimi görünür, `100%`-də tam qara kart alınır.
///
/// `colorScheme` qəsdən `.dark`: ağ mətn açıq rejimdə də oxunmalıdır.
///
/// iOS 16 yolu qalır, çünki tətbiqin deployment target-i hələ 16.0-dır.
private struct PrayerCardBackground: ViewModifier {
    let opacity: Double

    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content
                .environment(\.colorScheme, .dark)
                .containerBackground(for: .widget) { Color.black.opacity(opacity) }
        } else {
            content
                .environment(\.colorScheme, .dark)
                .padding(12)
                .background(Color.black.opacity(opacity))
        }
    }
}

extension View {
    func prayerCard(opacity: Double) -> some View {
        modifier(PrayerCardBackground(opacity: opacity))
    }
}

/// Live countdown plus the word next to it ("qaldı"), both sized alike.
private struct Countdown: View {
    let target: Date
    let label: String
    let size: CGFloat

    var body: some View {
        HStack(spacing: 4) {
            // ⚠️ İki tələ bir yerdə. `.fixedSize()` QOYMA: geri sayımın daxili ölçüsü qeyri-müəyyəndir
            // (mətn saniyədə dəyişir) və onu ideal ölçüyə sıxmaq vidcetin BÜTÜN məzmununu yox edir —
            // ekran qapqara qalır, nə çökmə, nə log, nə xəbərdarlıq (simulyatorda bisect ilə tapıldı).
            // Amma sərbəst buraxılanda mətn acgözdür: bütün eni yeyib «qaldı» sözünü kartın o biri
            // ucuna itələyir. Ona görə en şrift ölçüsünə nisbətlə məhdudlaşdırılır — «4:17:16» ən uzun
            // haldır və bu ölçüyə sığır.
            Text(timerInterval: Date()...target, countsDown: true)
                .font(.system(size: size).monospacedDigit())
                .foregroundStyle(.white.opacity(0.75))
                .lineLimit(1)
                .frame(maxWidth: size * 4.6, alignment: .trailing)

            Text(label)
                .font(.system(size: size))
                .foregroundStyle(.white.opacity(0.6))
                .lineLimit(1)
        }
    }
}

private struct EmptyState: View {
    let snapshot: PrayerSnapshot?

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: "location.slash")
                .foregroundStyle(.white.opacity(0.6))
            Text(snapshot?.noLocationLabel ?? "")
                .font(.footnote)
                .foregroundStyle(.white.opacity(0.8))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Small: the next prayer only — the counterpart of the Android 2x1 widget.
struct PrayerNextView: View {
    let entry: PrayerEntry

    var body: some View {
        let snapshot = entry.snapshot
        let next = snapshot?.next(after: entry.date)

        Group {
            if let snapshot, let next {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Image(systemName: PrayerWidgetStyle.symbol(for: next.icon))
                            .font(.system(size: 16))
                            .foregroundStyle(PrayerWidgetStyle.accent)

                        Text(next.label)
                            .font(.system(size: 15))
                            .foregroundStyle(.white.opacity(0.7))
                            .lineLimit(1)
                    }

                    Text(next.clock)
                        .font(.system(size: 30, weight: .medium))
                        .foregroundStyle(PrayerWidgetStyle.accent)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)

                    Countdown(target: next.date, label: snapshot.remainingLabel, size: 13)

                    if !snapshot.placeName.isEmpty {
                        Text(snapshot.placeName)
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.45))
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            } else {
                EmptyState(snapshot: snapshot)
            }
        }
        .prayerCard(opacity: cardOpacity(snapshot))
    }
}

/// Medium: every time of the day — the counterpart of the Android 5x1 widget.
struct PrayerTimesView: View {
    let entry: PrayerEntry

    var body: some View {
        let snapshot = entry.snapshot
        let day = snapshot?.day(containing: entry.date)
        let next = snapshot?.next(after: entry.date)

        Group {
            if let snapshot, let day, !day.items.isEmpty {
                VStack(spacing: 6) {
                    HStack(alignment: .firstTextBaseline) {
                        Text(next.map { "\($0.label) · \($0.clock)" } ?? snapshot.title)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                            .lineLimit(1)

                        Spacer(minLength: 6)

                        Text(day.dateLine)
                            .font(.system(size: 11))
                            .foregroundStyle(.white.opacity(0.55))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }

                    HStack(alignment: .top, spacing: 0) {
                        ForEach(day.items) { item in
                            let isNext = item.atMillis == next?.atMillis

                            VStack(spacing: 2) {
                                Image(systemName: PrayerWidgetStyle.symbol(for: item.icon))
                                    .font(.system(size: 13))
                                    .foregroundStyle(
                                        isNext ? PrayerWidgetStyle.accent : .white.opacity(0.55)
                                    )

                                Text(item.label)
                                    .font(.system(size: 11))
                                    .foregroundStyle(.white.opacity(0.6))
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.7)

                                Text(item.clock)
                                    .font(.system(size: 14, weight: isNext ? .bold : .regular))
                                    .foregroundStyle(
                                        isNext ? PrayerWidgetStyle.accent : .white.opacity(0.9)
                                    )
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.7)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }

                    if let next {
                        Countdown(target: next.date, label: snapshot.remainingLabel, size: 12)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                EmptyState(snapshot: snapshot)
            }
        }
        .prayerCard(opacity: cardOpacity(snapshot))
    }
}

/// The user's own setting, shared through the snapshot (Settings → Widgets → background).
///
/// Not named `opacity`: inside a view builder that resolves to SwiftUI's own `View.opacity(_:)`
/// modifier and the call fails with a type error that says nothing about the shadowing.
private func cardOpacity(_ snapshot: PrayerSnapshot?) -> Double {
    Double(snapshot?.backgroundOpacityPercent ?? 85) / 100.0
}
