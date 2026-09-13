package com.kaleem.screenassistant

import android.graphics.Bitmap

/**
 * In-memory handoff for the most recently captured screenshot.
 * CaptureService writes it, AssistantActivity reads it after the
 * ACTION_CAPTURE_READY broadcast. Not persisted — capture is
 * one-shot and short-lived by design (see PRD Security Requirements:
 * temporary bitmaps should be released when no longer needed).
 */
object CaptureHolder {
    var latestBitmap: Bitmap? = null
}
