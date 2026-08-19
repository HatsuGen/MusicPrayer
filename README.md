# musicPrayer Android

Offline Android music player rebuilt from the supplied HTML prototype. The current MVP includes:

- MediaStore library scanning and Room caching
- Media3 background playback through `MediaSessionService`
- Search, queue controls and seed-based Smart Shuffle
- USB Audio Class device detection
- A strict `UsbAudioEngine` boundary for a future native UAC1/UAC2 exclusive engine

## Important audio status

The current playback engine is Media3/ExoPlayer and **does not claim bit-perfect USB output**.
`UnsupportedUsbAudioEngine` intentionally prevents the UI from presenting a false exclusive-mode status.
A real UAPP-like route requires a tested native USB isochronous pipeline and DAC capability negotiation.

## Build

Open the root directory in a recent Android Studio, install Android SDK 36, use JDK 17, let Gradle sync, then run `app` on an Android 8+ device.

## Next implementation slice

1. Persist and request per-device USB permission.
2. Parse UAC descriptors and expose sample rates/bit depths.
3. Add the NDK PCM ring-buffer and exclusive USB transport.
4. Validate bit-perfect output with reference WAV files and DAC indicators.
5. Add FLAC/WAV feature extraction for RMS, centroid and BPM in WorkManager.
