# Screen Assistant — bluff prototype

Mimics assistant invocation + minimal UI. No OCR/capture wired yet — stubs only.

## What it does
1. `MainActivity` — requests "Display over other apps" permission, starts `OverlayService`.
2. `OverlayService` — pins an invisible ~70x90dp touch strip to the bottom-right corner (any app, system-wide). Swipe up from it past ~24dp threshold → launches `AssistantActivity`.
3. `AssistantActivity` — transparent bottom-sheet, matches the reference UI: rainbow dot + "Hi, how can I help?" + two pill buttons ("Search screen", "Translate") + rainbow accent bar. Tap outside the card to dismiss. Buttons currently just toast — wire real OCR/search pipeline into their click listeners.

## Run it
1. Open in Android Studio (Hedgehog+), let Gradle sync.
2. Run on device/emulator, API 26+.
3. Tap "Start corner-gesture assistant" → grant overlay permission → tap again.
4. Home out / open any app, swipe up from bottom-right corner.

## Known limitations (by design, for this stage)
- This is NOT real Android Assistant invocation (that needs `VoiceInteractionService` + being set as default assistant — heavier, more restricted path). Corner-swipe overlay is the practical stand-in.
- No screen capture yet (needs `MediaProjection`, Phase 1 in the reqs doc).
- No accessibility-service fallback for the gesture; pure touch-overlay only.
- Foreground service uses `specialUse` type — Play Store will want a justification string if this ever ships.

## Next per the reqs doc phases
- Phase 1: wire `MediaProjection` to actually capture the screen on trigger.
- Phase 2: region selection overlay + crop + magnifier.
- Phase 3: local OCR (ML Kit) behind the "Search screen"/"Translate" buttons.
