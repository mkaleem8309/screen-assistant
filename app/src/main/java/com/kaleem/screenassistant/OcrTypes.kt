package com.kaleem.screenassistant

import android.graphics.Rect

/**
 * One OCR-detected line of text with its position in the captured
 * screenshot's pixel coordinates. Line-level granularity — this matches
 * ML Kit's Text.Line, not the coarser TextBlock (which merges multiple
 * nearby lines into one paragraph and was too coarse for per-line overlay
 * positioning) — so each translated patch lines up with exactly the line
 * it replaces, the way Google Lens does it.
 */
data class OcrBlock(val text: String, val boundingBox: Rect)
