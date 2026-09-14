package com.kaleem.screenassistant

import android.graphics.Rect

/**
 * One OCR-detected line of text with its position in the captured
 * screenshot's pixel coordinates. Populated by TesseractOcr (RIL_TEXTLINE
 * level) — line-level granularity, so each translated patch lines up with
 * exactly the line it replaces, the way Google Lens does it.
 */
data class OcrBlock(val text: String, val boundingBox: Rect)
