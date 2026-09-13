package com.kaleem.screenassistant

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import java.io.File
import java.io.FileOutputStream

/**
 * The minimal "what can I do" sheet, launched by AssistantSession once the
 * system hands us a screenshot. No permission request, no capture step —
 * by the time this activity is on screen, CaptureHolder.latestBitmap is
 * already populated (or null, if the user disabled screenshots in the
 * system's assist settings). Region selection, OCR, and the real
 * search/translate actions are still to come.
 */
class AssistantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let content draw behind the status/nav bars so a full-display-sized
        // screenshot actually fills the window edge-to-edge instead of being
        // letterboxed inside the system-bar-inset content area.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_assistant)

        findViewById<View>(R.id.root_scrim).setOnClickListener { finish() }
        // Prevent taps on the card itself from bubbling to the scrim and closing it.
        findViewById<View>(R.id.assistant_card).setOnClickListener { }

        val capturedPreview = findViewById<ImageView>(R.id.captured_preview)
        val copyButton = findViewById<TextView>(R.id.btn_copy_screenshot)
        val bitmap = CaptureHolder.latestBitmap
        if (bitmap != null) {
            capturedPreview.setImageBitmap(bitmap)
            capturedPreview.visibility = View.VISIBLE
            capturedPreview.setOnClickListener { finish() }

            copyButton.visibility = View.VISIBLE
            copyButton.setOnClickListener { copyScreenshotToClipboard(bitmap) }
        }

        findViewById<TextView>(R.id.btn_search_screen).setOnClickListener {
            Toast.makeText(this, "TODO: region select \u2192 Google search", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btn_translate).setOnClickListener {
            Toast.makeText(this, "TODO: region select \u2192 OCR \u2192 translate", Toast.LENGTH_SHORT).show()
        }
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
