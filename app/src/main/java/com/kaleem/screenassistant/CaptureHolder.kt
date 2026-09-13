package com.kaleem.screenassistant

import android.graphics.Bitmap

/**
 * In-memory handoff for the most recently received screenshot.
 * AssistantSession writes it (straight from the system, no capture
 * permission involved), AssistantActivity reads it on launch. Not
 * persisted — capture is one-shot and short-lived by design (see PRD
 * Security Requirements: temporary bitmaps should be released when no
 * longer needed).
 */
object CaptureHolder {
    var latestBitmap: Bitmap? = null
}
