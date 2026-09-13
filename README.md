# BitAssist

Barebones: assistant-sheet UI + real Android assist invocation. Nothing else.

## What it does
1. `MainActivity` — launcher entry. Only job: a "Set as default assistant" button that
   opens Settings > Assist & voice input, where the user picks BitAssist as their default
   assistant. No API lets an app set this for itself, so this is just a shortcut to that
   screen — nothing else lives here.
2. `AssistantActivity` — the assistant sheet UI: rainbow dot + "Hi, how can I help?" + two
   pill buttons ("Search screen", "Translate") + rainbow accent bar. Tap outside the card
   to dismiss. Buttons currently just toast — wire the real OCR/search pipeline into their
   click listeners.
3. Invocation — `AssistantActivity` declares an `ACTION_ASSIST` intent filter, so once
   BitAssist is picked as default, the OS launches it directly on its own assist trigger
   (long-press home in 3-button nav, gesture-nav corner swipe, etc.) — no custom overlay.

## Run it
1. Open in Android Studio (Hedgehog+), let Gradle sync.
2. Install on device/emulator, API 26+.
3. Open BitAssist, tap "Set as default assistant", pick BitAssist in the list.
4. Trigger assist as normal for your device (long-press home / corner swipe / etc.).

## Known limitations (by design, for this stage)
- No screen capture yet (needs `MediaProjection`).
- No OCR pipeline — buttons are stubs.
- No `VoiceInteractionService` — this is the lightweight `ACTION_ASSIST` path, not a full
  voice-interaction replacement.
