# iOS screenshots

## `6.9/` — upload these

Six captures at **1320 × 2868**, the size App Store Connect requires for iPhone. Taken on iPhone 17
Pro Max, in Azerbaijani, **light appearance**, from the Debug simulator build on 2026-08-08:

| File | Screen |
|---|---|
| `01-home.png` | Home — Qur'an history, hadith history, bookmarks |
| `02-quran-index.png` | Sura index with the Arabic titles |
| `03-reader.png` | Reader — Arabic, tajweed colouring, Azerbaijani translation |
| `04-hadith.png` | Hadith library |
| `05-search.png` | Global search with its syntax hints |
| `06-settings.png` | Settings |

Supply 6.9" and App Store derives the rest, so this one set covers every iPhone.

## `6.5/` — for the 6.5-inch slot

The same six screens at **1284 × 2778**, which is what App Store Connect asks for when the iPhone
6.5-inch display slot is selected (it also accepts 1242 × 2688). Derived from the 6.9" set, not
re-shot: each image was first cropped by 12px of height so its aspect ratio matched the target
exactly, then scaled — so nothing is stretched. Text stays sharp because the source is larger than
the target.

Upload whichever set matches the slot the form is showing. If both slots are offered, 6.9" alone is
enough — App Store derives the smaller sizes from it.

## `ipad-13/` — upload these for iPad

Six captures at **2064 × 2752**, which is what App Store Connect requires for the **iPad 13-inch
display** slot. Taken on iPad Pro 13" (M5), in Azerbaijani, **light appearance**, from the Debug
simulator build on 2026-08-09 — after the iPad layout work (74th wave), so the readable-width
column and the two-pane reader are what the shots actually show.

| File | Screen |
|---|---|
| `01-home.png` | Home — verse of the day, Qur'an history, hadith history, bookmarks |
| `02-quran-index.png` | Sura index — **two columns**, Arabic titles |
| `03-reader.png` | Reader — **two-pane**: sura/verse navigator beside the text, tajweed + translation |
| `04-hadith.png` | Hadith — chapter list of *Kitāb al-Īmān*, two columns, Azerbaijani + Arabic |
| `05-search.png` | Global search — 200 hits with the match highlighted |
| `06-settings.png` | Settings — capped to a readable column, not stretched edge to edge |

Two deliberate differences from the 6.9" set, both because a 13" window shows more:

- `03-reader` shows the **two-pane** layout, which does not exist on a phone. It is the single
  strongest argument for iPad support, so it is worth the slot.
- `04-hadith` is one level deeper than the phone shot. The volumes root has only two rows; on a 13"
  screen that reads as an empty app, while the chapter list shows the library actually has content.

**These are only useful once the app is a universal binary.** While `TARGETED_DEVICE_FAMILY` is
`1`, App Store Connect does not offer an iPad slot at all. Captured with the flag passed to
`xcodebuild` as a one-off argument — `project.pbxproj` was **not** modified.

## `ipad-12.9/` — for the older 12.9-inch slot

The same six screens at **2048 × 2732**, the size App Store Connect asks for when the 12.9-inch iPad
Pro slot is shown. Derived from `ipad-13/` the same way `6.5/` was derived from `6.9/`: cropped by
1px of width so the aspect ratio matches exactly, then scaled down. Nothing is stretched.

If both slots are offered, 13" alone is enough — App Store derives the rest.

## `raw/` — reference only, not uploadable

The same six screens at 1206 × 2622 (iPhone 17, 6.3") in **dark appearance**, captured before the
Pro Max simulator was available. App Store Connect rejects these dimensions. They are kept only
because they show what the dark theme looks like — if dark is preferred for the listing, the 6.9"
set can be re-captured in dark in a couple of minutes rather than upscaled from here.

## These are raw captures, not marketing shots

No captions, device frames or ordering decisions have been made. App Store Connect accepts them
as-is. Worth remembering that the first two are what almost everyone actually sees — a store visitor
rarely swipes past them — so their order deserves a deliberate choice rather than the filename one.
