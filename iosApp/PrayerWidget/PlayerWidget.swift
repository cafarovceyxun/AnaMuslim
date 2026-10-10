//
//  PlayerWidget.swift
//  PrayerWidget
//
//  The recitation player, the iOS counterpart of Android's `RecitationPlayerWidgetUi.kt`: medium
//  (artwork, title, verse + reciter, verse transport), large (adds the verse text and the surah
//  jumps) and the two lock screen families. Buttons are `PlayerWidgetControlIntent`s, which iOS runs in the app's
//  process; a tap anywhere else opens the reader at the verse being recited.
//
//  Deliberately missing next to Android: the in-widget surah/verse picker. Android pages through it
//  inside the widget; on iOS a tap can only run an intent or open the app, and opening the surah
//  list from outside would push a tab root over another screen (CLAUDE.md: tab roots open only
//  through the bar).
//

import AppIntents
import SwiftUI
import WidgetKit

struct PlayerWidget: Widget {
    /// Must match `IosPlayerWidgetBridge.WIDGET_KIND`.
    let kind = "PlayerWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PlayerTimelineProvider()) { entry in
            PlayerWidgetView(
                snapshot: entry.snapshot,
                verseTextMode: entry.verseTextMode,
                verseZoomPercent: entry.verseZoomPercent
            )
        }
        .configurationDisplayName(LocalizedStringKey("widget.player.title"))
        .description(LocalizedStringKey("widget.player.description"))
        .supportedFamilies([.systemMedium, .systemLarge, .accessoryCircular, .accessoryRectangular])
    }
}

struct PlayerEntry: TimelineEntry {
    let date: Date
    let snapshot: PlayerSnapshot
    var verseTextMode: PlayerVerseTextMode = .both
    var verseZoomPercent: Int = 100
}

/// A−/A+ on the large widget. Like the text button, only this widget's look changes, so it runs in
/// the extension.
struct PlayerVerseZoomIntent: AppIntent {
    static var title: LocalizedStringResource = "Verse text size"
    static var isDiscoverable: Bool = false

    @Parameter(title: "Delta")
    var delta: Int

    init() {}

    init(delta: Int) {
        self.delta = delta
    }

    func perform() async throws -> some IntentResult {
        PlayerVerseZoom.step(by: delta)
        return .result()
    }
}

/// The large widget's text button. A plain `AppIntent`, not an audio one: it only changes what this
/// widget draws, so it runs in the extension and WidgetKit re-renders the widget once it returns —
/// no need to wake the app.
struct PlayerVerseTextModeIntent: AppIntent {
    static var title: LocalizedStringResource = "Verse text"
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        PlayerVerseTextMode.store(PlayerVerseTextMode.current.next)
        return .result()
    }
}

/// A single entry that never expires on its own: nothing about the player changes with time
/// alone. The app reloads this widget kind whenever the verse, play state or language changes.
struct PlayerTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> PlayerEntry {
        PlayerEntry(date: Date(), snapshot: PlayerSnapshotStore.placeholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (PlayerEntry) -> Void) {
        let snapshot = context.isPreview
            ? PlayerSnapshotStore.placeholder
            : (PlayerSnapshotStore.load() ?? PlayerSnapshotStore.placeholder)

        completion(PlayerEntry(date: Date(), snapshot: snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PlayerEntry>) -> Void) {
        let entry = PlayerEntry(
            date: Date(),
            snapshot: PlayerSnapshotStore.load() ?? PlayerSnapshotStore.placeholder,
            verseTextMode: PlayerVerseTextMode.current,
            verseZoomPercent: PlayerVerseZoom.current
        )
        completion(Timeline(entries: [entry], policy: .never))
    }
}

// MARK: - Views

struct PlayerWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let snapshot: PlayerSnapshot
    let verseTextMode: PlayerVerseTextMode
    let verseZoomPercent: Int

    var body: some View {
        Group {
            switch family {
            case .systemLarge:
                PlayerLargeView(snapshot: snapshot, verseTextMode: verseTextMode, verseZoomPercent: verseZoomPercent)
            case .accessoryCircular: PlayerCircularView(snapshot: snapshot)
            case .accessoryRectangular: PlayerRectangularView(snapshot: snapshot)
            default: PlayerMediumView(snapshot: snapshot)
            }
        }
        .widgetURL(snapshot.openURL)
        .prayerCard()
    }
}

/// B — artwork, title and verse/reciter on top; progress and verse transport below.
private struct PlayerMediumView: View {
    let snapshot: PlayerSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                ChapterArtwork(glyph: snapshot.chapterGlyph, size: 52)
                TitleBlock(snapshot: snapshot, subtitle: "\(snapshot.verseLabel) · \(snapshot.reciter)")
            }

            Spacer(minLength: 8)

            HStack(spacing: 10) {
                PlayerProgressBar(progress: snapshot.progress)
                ControlButton(.previousVerse, symbol: "backward.fill", label: snapshot.labels.previousVerse, size: 32)
                PlayPauseButton(snapshot: snapshot, size: 40)
                ControlButton(.nextVerse, symbol: "forward.fill", label: snapshot.labels.nextVerse, size: 32)
            }
        }
    }
}

/// D — header, the verse being recited (Arabic + translation), position, and the full transport
/// with surah jumps on the outside.
private struct PlayerLargeView: View {
    let snapshot: PlayerSnapshot
    let verseTextMode: PlayerVerseTextMode
    let verseZoomPercent: Int

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                ChapterArtwork(glyph: snapshot.chapterGlyph, size: 64)
                TitleBlock(snapshot: snapshot, subtitle: snapshot.reciter)
                Button(intent: PlayerVerseTextModeIntent()) {
                    ZStack {
                        Circle().fill(.quaternary)
                        Image(systemName: "character.book.closed")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.primary)
                    }
                    .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(snapshot.labels.verseText ?? "")
            }

            Spacer(minLength: 10)

            VerseText(snapshot: snapshot, mode: verseTextMode, zoom: CGFloat(verseZoomPercent) / 100)
                .frame(maxHeight: .infinity)

            Spacer(minLength: 10)

            VStack(spacing: 4) {
                PlayerProgressBar(progress: snapshot.progress)
                HStack {
                    Text(snapshot.verseLabel)
                    Spacer()
                    ZoomButton(label: "A−", delta: -1)
                    ZoomButton(label: "A+", delta: 1)
                    Spacer()
                    if snapshot.verseCount > 0 { Text("\(snapshot.verseCount)") }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }

            Spacer(minLength: 12)

            HStack(spacing: 14) {
                ControlButton(.previousChapter, symbol: "backward.end.alt.fill", label: snapshot.labels.previousChapter, size: 36)
                ControlButton(.previousVerse, symbol: "backward.fill", label: snapshot.labels.previousVerse, size: 44)
                PlayPauseButton(snapshot: snapshot, size: 58)
                ControlButton(.nextVerse, symbol: "forward.fill", label: snapshot.labels.nextVerse, size: 44)
                ControlButton(.nextChapter, symbol: "forward.end.alt.fill", label: snapshot.labels.nextChapter, size: 36)
            }
        }
    }
}

/// E — lock screen ring: surah progress around a play/pause button.
private struct PlayerCircularView: View {
    let snapshot: PlayerSnapshot

    var body: some View {
        ZStack {
            AccessoryWidgetBackground()
            Gauge(value: snapshot.progress) { EmptyView() }
                .gaugeStyle(.accessoryCircularCapacity)
            Button(intent: PlayerWidgetControlIntent(.toggle)) {
                Image(systemName: playSymbol(snapshot))
                    .font(.system(size: 18, weight: .semibold))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(snapshot.isPlaying ? snapshot.labels.pause : snapshot.labels.play)
        }
    }
}

/// E — lock screen line: title, verse position, progress.
private struct PlayerRectangularView: View {
    let snapshot: PlayerSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Image(systemName: snapshot.isPlaying ? "speaker.wave.2.fill" : "pause.fill")
                    .font(.caption2)
                Text(snapshot.title)
                    .font(.headline)
                    .lineLimit(1)
            }
            Text(snapshot.progressLabel)
                .font(.caption)
                .lineLimit(1)
            ProgressView(value: snapshot.progress)
                .progressViewStyle(.linear)
        }
        .widgetAccentable()
    }
}

// MARK: - Parts

/// A−/A+ — letter labels, so they read the same in every language.
private struct ZoomButton: View {
    let label: String
    let delta: Int

    var body: some View {
        Button(intent: PlayerVerseZoomIntent(delta: delta)) {
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(.primary)
                .frame(width: 32, height: 32)
                .background(Circle().fill(.quaternary))
        }
        .buttonStyle(.plain)
    }
}

/// Long verses shrink before they truncate; the translation gives way first.
private struct VerseText: View {
    let snapshot: PlayerSnapshot
    let mode: PlayerVerseTextMode
    /// A−/A+ multiplier. Zooming in also allows more lines; `minimumScaleFactor` still keeps an
    /// oversized verse from being cut off.
    let zoom: CGFloat

    var body: some View {
        // A text shown alone gets the room both would have shared: bigger and more lines.
        let alone = mode != .both
        let extraLines = max(0, Int(((zoom - 1) * 4).rounded(.up)))

        VStack(spacing: 8) {
            if mode.showsArabic && !snapshot.arabicText.isEmpty {
                Text(snapshot.arabicText)
                    .font(.system(size: (alone ? 26 : 22) * zoom))
                    .multilineTextAlignment(.center)
                    .environment(\.layoutDirection, .rightToLeft)
                    .lineLimit((alone ? 5 : 3) + extraLines)
                    .minimumScaleFactor(0.6)
                    .layoutPriority(1)
            }
            if mode.showsTranslation && !snapshot.translation.isEmpty {
                Text(snapshot.translation)
                    .font(.system(size: (alone ? 16 : 13) * zoom))
                    .foregroundStyle(alone ? .primary : .secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit((alone ? 7 : 4) + extraLines)
                    .minimumScaleFactor(0.75)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct TitleBlock: View {
    let snapshot: PlayerSnapshot
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(snapshot.title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.primary)
                .lineLimit(1)
            Text(subtitle)
                .font(.system(size: 12))
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The surah name in `suracon` lettering on the brand green — the Android artwork without its
/// photographic background, which would be a second copy of a large bitmap inside the extension.
private struct ChapterArtwork: View {
    let glyph: String
    let size: CGFloat

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.22, style: .continuous)
                .fill(PrayerWidgetStyle.accent)
            Text(glyph)
                .font(.custom("suracon", size: size * 0.62))
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(1)
                .minimumScaleFactor(0.2)
                .padding(size * 0.12)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

private struct PlayerProgressBar: View {
    let progress: Double

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(.tertiary)
                Capsule()
                    .fill(PrayerWidgetStyle.accent)
                    .frame(width: max(proxy.size.width * progress, progress > 0 ? 3 : 0))
            }
        }
        .frame(height: 3)
    }
}

private struct ControlButton: View {
    let action: PlayerWidgetAction
    let symbol: String
    let label: String
    let size: CGFloat

    init(_ action: PlayerWidgetAction, symbol: String, label: String, size: CGFloat) {
        self.action = action
        self.symbol = symbol
        self.label = label
        self.size = size
    }

    var body: some View {
        Button(intent: PlayerWidgetControlIntent(action)) {
            ZStack {
                Circle().fill(.quaternary)
                Image(systemName: symbol)
                    .font(.system(size: size * 0.36, weight: .semibold))
                    .foregroundStyle(.primary)
            }
            .frame(width: size, height: size)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

private struct PlayPauseButton: View {
    let snapshot: PlayerSnapshot
    let size: CGFloat

    var body: some View {
        Button(intent: PlayerWidgetControlIntent(.toggle)) {
            ZStack {
                Circle().fill(PrayerWidgetStyle.accent)
                Image(systemName: playSymbol(snapshot))
                    .font(.system(size: size * 0.4, weight: .bold))
                    // The system background colour: black on the light-green dark-mode accent,
                    // white on the deep-green light-mode one.
                    .foregroundStyle(Color(uiColor: .systemBackground))
            }
            .frame(width: size, height: size)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(snapshot.isPlaying ? snapshot.labels.pause : snapshot.labels.play)
    }
}

/// A widget cannot animate a spinner, so loading shows a static ellipsis instead of play/pause.
private func playSymbol(_ snapshot: PlayerSnapshot) -> String {
    if snapshot.isLoading { return "ellipsis" }
    return snapshot.isPlaying ? "pause.fill" : "play.fill"
}
