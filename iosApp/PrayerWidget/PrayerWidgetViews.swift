//
//  PrayerWidgetViews.swift
//  PrayerWidget
//

import SwiftUI
import UIKit
import WidgetKit

enum PrayerWidgetStyle {
    /// Brend yaşılı, rejimə görə iki tonda.
    ///
    /// Fon artıq sistemin öz lövhəsidir (aşağıdakı [PrayerCardBackground]) — o isə işıqlı rejimdə
    /// **ağdır**, ona görə Android-dəki açıq yaşıl tək başına kifayət etmir: ağ lövhədə tünd ton
    /// işlədilir.
    static let accent = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x19 / 255, green: 0xB3 / 255, blue: 0x7E / 255, alpha: 1)
            : UIColor(red: 0x0B / 255, green: 0x6B / 255, blue: 0x4A / 255, alpha: 1)
    })

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

/// Vidcet fonu — **qəsdən öz fonumuz yoxdur**.
///
/// ⚠️ Vidcet həqiqətən şəffaf/şüşə OLA BİLMİR, bunu təkrar sınama. Simulyatorda (iOS 26.5, iPhone 17
/// Pro) dörd yol da ayrı-ayrı build-lərlə yoxlandı, hamısında kart qeyri-şəffaf qaldı və divar kağızı
/// ondan görünmədi: `.ultraThinMaterial` (düz boz), `.fill.tertiary` (tünd boz), `Color.clear` (sistem
/// lövhəsi) və hətta iOS 26-nın öz `glassEffect()`-i (qara). Səbəb: ana ekranda vidcetin altında divar
/// kağızı yox, sistemin öz **qeyri-şəffaf lövhəsi** durur, ona görə fondakı alfa divar kağızı ilə
/// deyil, həmin lövhə ilə qarışır — nə qədər şəffaf versən, nəticə eynidir.
///
/// Ona görə fon bütövlükdə **sistemə buraxılır**: `Color.clear` verilir, öz qatımız çəkilmir. Bunun
/// qazancı odur ki, vidcet sistemin öz vidcetləri kimi davranır — qaranlıq rejimdə tünd, işıqlı
/// rejimdə açıq lövhə, iOS 26-nın **«Clear» ana ekran görünüşündə** isə sistem lövhənin yerinə
/// həqiqi şüşə çəkir. Şüşəyə yeganə real yol budur, və əvvəlki qara qat məhz onu bloklayırdı.
///
/// Elə buna görə mətn rəngləri `.primary`/`.secondary` olmalıdır: sabit ağ mətn işıqlı lövhədə itir.
/// Eyni səbəbdən `colorScheme` **məcbur edilmir** — lövhə onsuz da sistemin rejimindədir.
///
/// Deployment target 2026-09-17-də **17.0-a sabitləndi**, ona görə köhnə iOS 16 qolu (padding-lə
/// əvəzləmə) silindi — `containerBackground` artıq hər dəstəklənən versiyada var. Rəqəmi orada
/// `$(RECOMMENDED_IPHONEOS_DEPLOYMENT_TARGET)` kimi saxlama: o, SDK ilə sürüşür (Xcode 27 onu
/// 16.0-dan 17.0-a qaldırmışdı) və minimum versiyanı xəbərsiz dəyişir.
private struct PrayerCardBackground: ViewModifier {
    func body(content: Content) -> some View {
        content.containerBackground(for: .widget) { Color.clear }
    }
}

extension View {
    func prayerCard() -> some View {
        modifier(PrayerCardBackground())
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
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .frame(maxWidth: size * 4.6, alignment: .trailing)

            Text(label)
                .font(.system(size: size))
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }
}

private struct EmptyState: View {
    let snapshot: PrayerSnapshot?

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: "location.slash")
                .foregroundStyle(.secondary)
            Text(snapshot?.noLocationLabel ?? "")
                .font(.footnote)
                .foregroundStyle(.secondary)
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
                            .foregroundStyle(.secondary)
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
                            .foregroundStyle(.tertiary)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            } else {
                EmptyState(snapshot: snapshot)
            }
        }
        .prayerCard()
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
                            .foregroundStyle(.primary)
                            .lineLimit(1)

                        Spacer(minLength: 6)

                        Text(day.dateLine)
                            .font(.system(size: 11))
                            .foregroundStyle(.secondary)
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
                                        isNext ? PrayerWidgetStyle.accent : Color.secondary
                                    )

                                Text(item.label)
                                    .font(.system(size: 11))
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.7)

                                Text(item.clock)
                                    .font(.system(size: 14, weight: isNext ? .bold : .regular))
                                    .foregroundStyle(
                                        isNext ? PrayerWidgetStyle.accent : Color.primary
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
        .prayerCard()
    }
}
