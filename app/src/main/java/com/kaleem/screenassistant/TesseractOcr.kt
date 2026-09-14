package com.kaleem.screenassistant

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * OCR via Tesseract instead of ML Kit's Text Recognition, specifically
 * because ML Kit only supports Latin/Chinese/Devanagari/Japanese/Korean
 * script — no Arabic, at any download size. Tesseract supports Arabic (and
 * ~100 other languages) via a per-language trained-data file, downloaded
 * once and cached under the app's private files dir.
 *
 * Initialized with a combined "eng+ara" model so a single pass handles
 * either script (or a mix) without having to guess which language to use
 * before OCR has actually run. Only these two languages are wired up right
 * now, matching what's actually been tested — adding another is just
 * adding its Tesseract code to LANGUAGES/TESS_LANG below, the
 * download/init/recognize plumbing already supports any Tesseract-trained
 * language.
 *
 * Known simplification: the ResultIterator isn't explicitly disposed after
 * use. Tesseract4Android's Java wrapper doesn't clearly document a manual
 * free for it (unlike TessBaseAPI itself, which we deliberately keep and
 * reuse across calls rather than recreate). Worth revisiting if repeated
 * translate calls in one session start showing memory pressure.
 */
object TesseractOcr {

    private const val TAG = "TesseractOcr"
    private val LANGUAGES = listOf("eng", "ara")
    private const val TESS_LANG = "eng+ara"
    private const val TESSDATA_BASE_URL =
        "https://github.com/tesseract-ocr/tessdata_fast/raw/main/"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var cachedApi: TessBaseAPI? = null

    fun recognize(
        context: Context,
        bitmap: Bitmap,
        onProgress: (String) -> Unit,
        onResult: (List<OcrBlock>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Thread {
            try {
                val tessDir = ensureTrainedData(context) { status ->
                    mainHandler.post { onProgress(status) }
                }

                val api = cachedApi ?: TessBaseAPI().apply {
                    if (!init(tessDir.absolutePath, TESS_LANG)) {
                        throw IllegalStateException("Tesseract failed to initialize")
                    }
                    pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
                }.also { cachedApi = it }

                api.setImage(bitmap)
                // Forces full-page recognition to run and populates the
                // internal structures resultIterator reads from below.
                api.utF8Text

                val blocks = mutableListOf<OcrBlock>()
                val iterator = api.resultIterator
                if (iterator != null) {
                    iterator.begin()
                    do {
                        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                        val text = iterator.getUTF8Text(level)
                        val rect = iterator.getBoundingRect(level)
                        if (!text.isNullOrBlank() && rect != null) {
                            blocks.add(OcrBlock(text.trim(), rect))
                        }
                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                }

                api.clear()
                mainHandler.post { onResult(blocks) }
            } catch (e: Exception) {
                Log.e(TAG, "Tesseract OCR failed", e)
                mainHandler.post { onError(e) }
            }
        }.start()
    }

    private fun ensureTrainedData(context: Context, onProgress: (String) -> Unit): File {
        val tessDir = File(context.filesDir, "tesseract")
        val dataDir = File(tessDir, "tessdata").apply { mkdirs() }
        for (lang in LANGUAGES) {
            val file = File(dataDir, "$lang.traineddata")
            if (!file.exists()) {
                onProgress("Downloading $lang language pack\u2026")
                val tmp = File(dataDir, "$lang.traineddata.tmp")
                URL("$TESSDATA_BASE_URL$lang.traineddata").openStream().use { input ->
                    FileOutputStream(tmp).use { output -> input.copyTo(output) }
                }
                tmp.renameTo(file)
            }
        }
        return tessDir
    }
}
