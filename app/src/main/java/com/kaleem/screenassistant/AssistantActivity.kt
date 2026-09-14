package com.kaleem.screenassistant

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * The minimal "what can I do" sheet, launched by AssistantSession once the
 * system hands us a screenshot. No permission request, no capture step —
 * by the time this activity is on screen, CaptureHolder.latestBitmap is
 * already populated (or null, if the user disabled screenshots in the
 * system's assist settings).
 *
 * "Search screen" is a TODO stub — its real job is region-select then
 * Google search on that region, not OCR, so it doesn't touch OCR at all.
 *
 * "Translate" is the Lens-style flow: OCR with per-line positions (via
 * Tesseract — see TesseractOcr.kt for why, not ML Kit), detect the source
 * language, translate each line on-device, and render the translated text
 * directly over where the original line was.
 */
class AssistantActivity : AppCompatActivity() {

    // Full set of languages ML Kit's on-device translator actually supports —
    // this is the built-in "source" for available languages: no network
    // call, no external API, just the library's own catalog.
    private val targetLanguages: List<Pair<String, String>> by lazy {
        TranslateLanguage.getAllLanguages()
            .map { code -> displayName(code) to code }
            .sortedBy { it.first }
    }
    private var targetLanguage = TranslateLanguage.ENGLISH
    private var targetLanguageLabel = "English"

    // Cached so switching the target language re-translates instead of
    // re-running OCR + language detection from scratch.
    private var cachedBlocks: List<OcrBlock>? = null
    private var cachedSourceLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let content draw behind the status/nav bars so a full-display-sized
        // screenshot actually fills the window edge-to-edge instead of being
        // letterboxed inside the system-bar-inset content area.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_assistant)

        val capturedPreview = findViewById<ImageView>(R.id.captured_preview)
        val dimOverlay = findViewById<View>(R.id.dim_overlay)
        val copyButton = findViewById<TextView>(R.id.btn_copy_screenshot)
        val languageButton = findViewById<TextView>(R.id.btn_target_language)

        val bitmap = CaptureHolder.latestBitmap
        if (bitmap != null) {
            capturedPreview.setImageBitmap(bitmap)
            capturedPreview.visibility = View.VISIBLE
            dimOverlay.visibility = View.VISIBLE

            copyButton.visibility = View.VISIBLE
            copyButton.setOnClickListener { copyScreenshotToClipboard(bitmap) }
        }

        findViewById<TextView>(R.id.btn_search_screen).setOnClickListener {
            Toast.makeText(this, "TODO: draw a rectangle \u2192 Google search", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btn_translate).setOnClickListener {
            val current = CaptureHolder.latestBitmap
            if (current == null) {
                Toast.makeText(this, "No screenshot available", Toast.LENGTH_SHORT).show()
            } else {
                startTranslateFlow(current)
            }
        }

        languageButton.setOnClickListener { showLanguagePicker() }
    }

    // ---- Translate: Lens-style positioned overlay ----

    private fun startTranslateFlow(bitmap: Bitmap) {
        TesseractOcr.recognize(
            context = this,
            bitmap = bitmap,
            onProgress = { status -> Toast.makeText(this, status, Toast.LENGTH_SHORT).show() },
            onResult = { blocks ->
                if (blocks.isEmpty()) {
                    Toast.makeText(this, "No text found on screen", Toast.LENGTH_SHORT).show()
                } else {
                    cachedBlocks = blocks
                    detectLanguageAndTranslate(bitmap, blocks)
                }
            },
            onError = {
                Toast.makeText(this, "OCR failed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun detectLanguageAndTranslate(bitmap: Bitmap, blocks: List<OcrBlock>) {
        val sample = blocks.joinToString(" ") { it.text }.take(200)
        LanguageIdentification.getClient().identifyLanguage(sample)
            .addOnSuccessListener { code ->
                val sourceLang = if (code == "und") TranslateLanguage.ENGLISH else code
                cachedSourceLanguage = sourceLang
                translateAndRender(bitmap, blocks, sourceLang)
            }
            .addOnFailureListener {
                val fallback = TranslateLanguage.ENGLISH
                cachedSourceLanguage = fallback
                translateAndRender(bitmap, blocks, fallback)
            }
    }

    private fun translateAndRender(bitmap: Bitmap, blocks: List<OcrBlock>, sourceLang: String) {
        val container = findViewById<FrameLayout>(R.id.translation_overlay_container)
        val imageView = findViewById<ImageView>(R.id.captured_preview)
        TranslationOverlay.render(
            context = this,
            container = container,
            blocks = blocks,
            bitmap = bitmap,
            viewWidth = imageView.width,
            viewHeight = imageView.height,
            sourceLanguage = sourceLang,
            targetLanguage = targetLanguage,
            onDone = {
                container.visibility = View.VISIBLE
                findViewById<View>(R.id.btn_target_language).visibility = View.VISIBLE
            },
            onError = {
                Toast.makeText(
                    this,
                    "Couldn't download the language pack \u2013 check your connection",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showLanguagePicker() {
        val labels = targetLanguages.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Translate to")
            .setItems(labels) { _, which ->
                targetLanguageLabel = targetLanguages[which].first
                targetLanguage = targetLanguages[which].second
                findViewById<TextView>(R.id.btn_target_language).text = "\u2192 $targetLanguageLabel"

                val bitmap = CaptureHolder.latestBitmap
                val blocks = cachedBlocks
                val sourceLang = cachedSourceLanguage
                if (bitmap != null && blocks != null && sourceLang != null) {
                    translateAndRender(bitmap, blocks, sourceLang)
                }
            }
            .show()
    }

    // ---- Screenshot clipboard copy (unchanged from before) ----

    private fun displayName(languageCode: String): String {
        val name = Locale(languageCode).displayLanguage
        // Fall back to the raw code for the handful of ML Kit codes Locale
        // doesn't recognize, rather than showing an empty label.
        return name.ifBlank { languageCode }.replaceFirstChar { it.uppercase() }
    }

    private fun copyScreenshotToClipboard(bitmap: Bitmap) {
        try {
            val dir = File(cacheDir, "captures").apply { mkdirs() }
            val file = File(dir, "capture.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val clip = ClipData.newUri(contentResolver, "Screenshot", uri)
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            Toast.makeText(this, "Screenshot copied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't copy screenshot", Toast.LENGTH_SHORT).show()
        }
    }
}
