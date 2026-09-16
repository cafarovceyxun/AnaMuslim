# Privacy Policy

_Last updated: 2026-09-16_

AnaMuslim is an ad-free, privacy-focused Qur'an and hadith application. We do not
run our own analytics, advertising, or tracking servers, and the app does not
require an account.

## Data the app stores on your device

- Reading position, bookmarks (with your own notes), and reading history for
  both the Qur'an and hadith
- App preferences (theme, language, fonts, selected translations, audio and
  reminder settings)
- Downloaded content (fonts, translations, recitations, hadith database)
- Crash logs, kept locally; they are only shared if you choose to copy them into
  a bug report yourself

This data stays **on your device**. It is not transmitted to us.

## Network connections

The app connects to remote services **only to fetch content you request**
(translations, scripts, recitations, word-by-word data, hadith, and search).
These providers include:

- Qur'an.com (`api.quran.com`, `audio.qurancdn.com`)
- AlfaazPlus (`api.alfaazplus.com`, `gh-proxy.alfaazplus.com`)
- GitHub / jsDelivr / QuranicAudio for on-demand assets
- The project's own Supabase backend (`molyqwcaynvsdmixtcbc.supabase.co`) for
  the hadith library, the Azerbaijani translation, the daily verse/hadith,
  verse reports, and the qibla map tiles (see below)

When you make such a request, your IP address and the requested resource are
visible to the relevant provider, as with any normal internet request. Each
provider has its own privacy policy. AnaMuslim does not attach any identifier to
these requests.

Sign-in exists only for the project's own maintainers, to edit content and review
reports. Ordinary use never requires an account.

## Optional user-submitted reports

If you use the in-app **verse report** feature, the verse reference and the text
you write are sent anonymously to the project's Supabase backend so a maintainer
can review the translation. No account, name, or device identifier is attached.

If you use the in-app **suggestions** board, the text you write, the category
you pick, and the app version and platform are sent to the same backend for
review before anyone else can see them. **No identifier of any kind is stored
with a suggestion** — no account, no name, no device id. Each submission gets a
random receipt that is kept only on your device so the app can show you the
status of what you sent; deleting the app deletes those receipts. Votes are
counted as a plain number on the suggestion: which suggestions you voted for is
remembered **on your device only** and never sent as part of your identity.

The **bug report** and crash-log actions open a GitHub issue form in your
browser. Anything you choose to submit there is public and governed by GitHub's
terms and privacy policy.

## Location (prayer times)

Prayer times need to know roughly where you are. The app asks for location
**only while you are using it** — it **never** asks for background location.

Since the qibla feature was added, the app asks for **precise** location as well as
approximate (`ACCESS_FINE_LOCATION` plus `ACCESS_COARSE_LOCATION` on Android, "When
In Use" on iOS). The reason is specific and measurable: Android rounds approximate
location to a grid roughly 2 km across, and the value changes between readings. For
prayer times that is harmless — one minute of time is about 25 km. For the qibla
near the Kaaba it is fatal: measured in Makkah, two consecutive approximate readings
sat 2 km apart and moved the qibla direction by 62 degrees.

You can still refuse precise location, or grant only "approximate". Everything except
the qibla keeps working, and the qibla screen says plainly that the direction may be
wrong rather than showing a confident but false arrow.

- The times themselves are computed **on the phone** from the coordinates and the
  date. There is no prayer-time server, and the calculation needs no network at
  all — it works in airplane mode.
- Your coordinates are stored **on your device** and are never uploaded to us.
- **One exception, and only when you ask for it:** when you tap "Use my location",
  the app asks the operating system to turn those coordinates into a place name
  (Android's `Geocoder`, backed by Google; iOS's `CLGeocoder`, backed by Apple).
  That single lookup sends the coordinates to the platform provider under their
  own privacy policy. It happens once per location you set, never in the
  background, and never for the prayer times themselves. If it fails or you are
  offline, the app simply shows the coordinates instead and everything else keeps
  working. Choosing a city from the built-in list does not do this lookup at all.
- Location is read **only while the app is open**, when you tap "Use my location".
  Notifications and the widget use the stored coordinates; they never request a
  new fix.
- You can skip the permission entirely: the city list ships inside the app and
  works offline, and coordinates can be typed by hand.
- Coordinates are deliberately **excluded from the settings backup file**, so an
  export made in one city cannot silently produce wrong times on a phone in
  another.

## Qibla (map and compass)

The **compass** uses the phone's own magnetic sensor. Nothing about it leaves the
device. The correction from magnetic north to true north is computed on the phone
from a public-domain model (the NOAA/BGS World Magnetic Model) that ships inside
the app, so the compass needs no network at all.

The **map** shows street or satellite imagery. Those tiles are fetched **through
the project's own Supabase function, never directly from the map provider**. That
is the whole point of the arrangement: the imagery provider never sees your IP
address or which tiles you looked at — it only sees a request arriving from the
project's server. The function is written to keep no request log; for abuse
protection it holds a short-lived hash of the caller's address in memory for one
minute and then discards it. (Supabase's own platform-level logging is a separate
matter and is kept to the minimum the platform allows.)

Tiles you have already seen are cached on your device, so re-opening the map over
the same area sends nothing at all, and the map keeps working offline.

The map starts at the location you already set for prayer times. If you drag the
pin to line it up with your own building, that adjusted position is stored **on
your device only** and is never uploaded. The pin affects the map only — the
compass always uses your actual position, not where you dragged the map to.

## Notifications

The daily verse reminder and prayer-time reminders are off by default. If you
turn either on, the app asks for notification permission and schedules them
**on your device** — no push service is involved and nothing is sent to a server.

## No third-party analytics or ads

The app contains no advertising SDKs and no third-party analytics SDKs.

## Children's privacy

The app collects no personal information and is suitable for all ages.

## Changes

This policy may be updated; changes will be reflected in this file with a new
date. For questions, open an issue at
https://github.com/cafarovceyxun/AnaMuslim/issues.
