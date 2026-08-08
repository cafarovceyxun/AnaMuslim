# iOS screenshots

`raw/` holds six unretouched captures of the running app, in Azerbaijani, taken from the Debug
simulator build on 2026-08-08: home, Qur'an index, reader (tajweed + Azerbaijani translation),
hadith library, search, settings.

## ⚠️ These are 6.3", not the 6.9" App Store Connect requires

| | Pixels | Device |
|---|---|---|
| Captured here | 1206 × 2622 | iPhone 17 (6.3") |
| App Store needs | 1320 × 2868 | iPhone 17 Pro Max (6.9") |

The 6.9" set was not captured because the simulator-access request for iPhone 17 Pro Max was never
answered. The Max simulator is already booted and already has the app installed with a populated
data container copied across, so re-capturing is only a matter of granting access
("Let Claude use it" in the simulator panel) and repeating the same six taps.

The aspect ratios are near-identical (0.4600 vs 0.4603), so upscaling `raw/` to 1320 × 2868 would
technically pass validation — but it costs ~9% sharpness on a store page, which is the one place
that matters. Prefer re-capturing.

## Before uploading

These are raw device captures, not marketing shots — no captions, frames or ordering decisions have
been made. App Store Connect accepts them as-is, but the first two screenshots are what almost
everyone actually sees, so they are worth choosing deliberately.
