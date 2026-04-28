# FireTV-librepods

A Fire TV port of [LibrePods](https://github.com/kavishdevar/librepods) that
brings two AirPods-friendly behaviors to Amazon Fire TV devices:

1. **Auto audio routing on connect** — when AirPods (or other Bluetooth
   earbuds) connect, Fire TV media output switches to them; on disconnect,
   audio falls back to TV speakers / HDMI ARC.
2. **Ear-detection auto-pause** — playback pauses when a bud is removed and
   resumes when it is re-inserted.

> **Status:** scaffolding (phase 1). Nothing is shippable yet.

## Builds and distribution

This project ships **two product flavors** from one codebase:

| Flavor | Where | Requires root? | Ear-detection source |
|---|---|---|---|
| `appstoreLite` | Amazon Appstore (free) | No | BLE manufacturer-specific advertisements (best-effort) |
| `rootedFull`   | GitHub Releases (sideload) | Yes (rooted Fire OS) | Apple Accessory Protocol over L2CAP (full fidelity) |

Amazon Appstore policy does not allow apps that require root, hence the
split. Both flavors share the same UI and service code; only the
Bluetooth integration layer differs.

## Installation (sideload, `rootedFull`)

1. Enable **Apps from Unknown Sources** under
   *Settings → My Fire TV → Developer Options*.
2. Install the [Downloader](https://www.aftvnews.com/downloader/) app from
   the Amazon Appstore.
3. In Downloader, enter the URL of the latest `*-rootedFull-release.apk`
   from the [GitHub Releases](../../releases) page.
4. Open the app from the Fire TV launcher.

## Build from source

```bash
git clone --recurse-submodules https://github.com/aniketphatak/firetv-librepods.git
cd firetv-librepods
./gradlew :tv-app:assembleAppstoreLiteDebug
./gradlew :tv-app:assembleRootedFullDebug
```

Requires JDK 21 and Android SDK with platforms `android-34` installed.

## Supported AirPods models

Inherited from upstream LibrePods. Full feature support requires
`rootedFull` and one of:

- AirPods Pro (2nd Gen)
- AirPods Pro (3rd Gen) — except heart-rate
- AirPods Max

Other AirPods get basic features (battery / connection state) on either
flavor.

## License

GPL-3.0-or-later — see [LICENSE](./LICENSE).

This is an independent port of [kavishdevar/librepods](https://github.com/kavishdevar/librepods).
See [NOTICE](./NOTICE) for full attribution. Apple, AirPods, Fire TV, and
Fire OS are trademarks of their respective owners; this project is not
affiliated with or endorsed by Apple or Amazon.
