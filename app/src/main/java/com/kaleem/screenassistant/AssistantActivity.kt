package com.kaleem.screenassistant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
        setContentView(R.layout.activity_assistant)

        findViewById<View>(R.id.root_scrim).setOnClickListener { finish() }
        // Prevent taps on the card itself from bubbling to the scrim and closing it.
        findViewById<View>(R.id.assistant_card).setOnClickListener { }

        val capturedPreview = findViewById<ImageView>(R.id.captured_preview)
        val bitmap = CaptureHolder.latestBitmap
        if (bitmap != null) {
            capturedPreview.setImageBitmap(bitmap)
            capturedPreview.visibility = View.VISIBLE
            capturedPreview.setOnClickListener { finish() }
        }

        findViewById<TextView>(R.id.btn_search_screen).setOnClickListener {
            Toast.makeText(this, "TODO: region select \u2192 Google search", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btn_translate).setOnClickListener {
            Toast.makeText(this, "TODO: region select \u2192 OCR \u2192 translate", Toast.LENGTH_SHORT).show()
        }
    }
}
