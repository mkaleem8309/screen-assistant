package com.kaleem.screenassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Renders translated text directly over the region it came from, on top of
 * the original screenshot — Lens-style — instead of dumping everything into
 * one separate text panel.
 *
 * Blocks are translated one at a time, in top-to-bottom order, rather than
 * all at once: that makes the basic collision avoidance below deterministic
 * (each new patch only ever has to check against patches already placed
 * above it). It costs a bit of latency versus firing all translations in
 * parallel, which is an acceptable trade for correctness at this stage —
 * ML Kit's on-device translate is fast enough per call that a normal page
 * still finishes in well under a couple of seconds.
 */
object TranslationOverlay {

    fun render(
        context: Context,
        container: FrameLayout,
        blocks: List<OcrBlock>,
        bitmap: Bitmap,
        viewWidth: Int,
        viewHeight: Int,
        sourceLanguage: String,
        targetLanguage: String,
        onDone: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        container.removeAllViews()
        if (blocks.isEmpty() || viewWidth == 0 || viewHeight == 0) {
            onDone()
            return
        }

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
        )

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                // The ImageView uses centerCrop: figure out the same scale+offset
                // it applies, so a block's bitmap-space Rect lands in the right
                // spot in view-space.
                val bitmapAspect = bitmap.width.toFloat() / bitmap.height
                val viewAspect = viewWidth.toFloat() / viewHeight
                val scale: Float
                val dx: Float
                val dy: Float
                if (bitmapAspect > viewAspect) {
                    scale = viewHeight.toFloat() / bitmap.height
                    dx = (bitmap.width * scale - viewWidth) / 2f
                    dy = 0f
                } else {
                    scale = viewWidth.toFloat() / bitmap.width
                    dx = 0f
                    dy = (bitmap.height * scale - viewHeight) / 2f
                }

                val ordered = blocks.sortedBy { it.boundingBox.top }
                translateSequential(
                    translator, ordered, 0, context, container,
                    scale, dx, dy, viewWidth, viewHeight, mutableListOf(), onDone
                )
            }
            .addOnFailureListener(onError)
    }

    private fun translateSequential(
        translator: Translator,
        blocks: List<OcrBlock>,
        index: Int,
        context: Context,
        container: FrameLayout,
        scale: Float,
        dx: Float,
        dy: Float,
        viewWidth: Int,
        viewHeight: Int,
        placedRects: MutableList<Rect>,
        onDone: () -> Unit
    ) {
        if (index >= blocks.size) {
            onDone()
            return
        }
        val block = blocks[index]
        translator.translate(block.text)
            .addOnSuccessListener { translated ->
                placePatch(
                    context, container, block.boundingBox, translated,
                    scale, dx, dy, viewWidth, viewHeight, placedRects
                )
                translateSequential(
                    translator, blocks, index + 1, context, container,
                    scale, dx, dy, viewWidth, viewHeight, placedRects, onDone
                )
            }
            .addOnFailureListener {
                // Skip this one line rather than failing the whole page.
                translateSequential(
                    translator, blocks, index + 1, context, container,
                    scale, dx, dy, viewWidth, viewHeight, placedRects, onDone
                )
            }
    }

    private fun placePatch(
        context: Context,
        container: FrameLayout,
        box: Rect,
        text: String,
        scale: Float,
        dx: Float,
        dy: Float,
        viewWidth: Int,
        viewHeight: Int,
        placedRects: MutableList<Rect>
    ) {
        val heightPx = box.height() * scale

        // Scale the font to roughly match the original line's height, like
        // Lens does, instead of a fixed size that's wrong for most content.
        val density = context.resources.displayMetrics.scaledDensity
        val textSizeSp = (heightPx / density * 0.62f).coerceIn(10f, 22f)

        val patch = TextView(context).apply {
            this.text = text
            setTextColor(0xFF1C1C1E.toInt())
            textSize = textSizeSp
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.translation_patch_bg)
            setPadding(14, 4, 14, 4)
            elevation = 6f
            setTextIsSelectable(true)
        }

        // Cap width so a long translation wraps to multiple lines instead of
        // running off-screen or blowing straight through its neighbors.
        // TextView wraps automatically once its measured width is
        // constrained below the text's natural single-line width.
        val lineWidthPx = (box.width() * scale).toInt().coerceAtLeast(1)
        val maxWidthPx = (viewWidth * 0.9f).toInt().coerceAtLeast(lineWidthPx)
        patch.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidthPx, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        // Stretch to at least the original line's own width — a short
        // translation shouldn't shrink into a tiny centered chip that
        // ignores the width the source text actually occupied; it should
        // spread across roughly the same span, the way Lens keeps patches
        // matched to their source line's footprint.
        val finalWidth = patch.measuredWidth.coerceAtLeast(lineWidthPx)
        patch.measure(
            View.MeasureSpec.makeMeasureSpec(finalWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val measuredWidth = finalWidth
        val measuredHeight = patch.measuredHeight

        // Center the patch on the original line's own center, in both axes.
        // This works the same regardless of whether the source line reads
        // left-to-right or right-to-left — no hardcoded side or coordinates,
        // just "sit where the line it replaces sat."
        val centerX = box.centerX() * scale - dx
        val centerY = box.centerY() * scale - dy
        var left = (centerX - measuredWidth / 2f).toInt()
        var top = (centerY - measuredHeight / 2f).toInt()

        // Basic collision avoidance: nudge straight down, a small step at a
        // time, until this patch stops overlapping an already-placed one.
        // Not real layout logic — just enough that normal content doesn't
        // visibly stack on itself. Easy spot to swap in something smarter
        // later without touching the rest of this file.
        val stepPx = (4 * density).toInt().coerceAtLeast(1)
        var rect = Rect(left, top, left + measuredWidth, top + measuredHeight)
        var attempts = 0
        while (placedRects.any { Rect.intersects(it, rect) } && attempts < 25) {
            top += stepPx
            rect = Rect(left, top, left + measuredWidth, top + measuredHeight)
            attempts++
        }

        left = left.coerceIn(0, (viewWidth - measuredWidth).coerceAtLeast(0))
        top = top.coerceIn(0, (viewHeight - measuredHeight).coerceAtLeast(0))
        placedRects.add(Rect(left, top, left + measuredWidth, top + measuredHeight))

        val params = FrameLayout.LayoutParams(measuredWidth, measuredHeight).apply {
            leftMargin = left
            topMargin = top
        }
        container.addView(patch, params)
    }
}
