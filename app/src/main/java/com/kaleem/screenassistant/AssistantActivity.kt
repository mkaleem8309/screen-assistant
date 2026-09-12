package com.kaleem.screenassistant

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * The minimal "what can I do" sheet shown after the corner-swipe gesture.
 * Action buttons are stubs for now — wire OCR/translate pipelines in here
 * once those modules exist.
 */
class AssistantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        findViewById<android.view.View>(R.id.root_scrim).setOnClickListener {
            finish()
        }
        // Prevent taps on the card itself from bubbling to the scrim and closing it.
        findViewById<android.view.View>(R.id.assistant_card).setOnClickListener { }

        findViewById<TextView>(R.id.btn_search_screen).setOnClickListener {
            Toast.makeText(this, "TODO: capture region \u2192 Google search", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btn_translate).setOnClickListener {
            Toast.makeText(this, "TODO: capture region \u2192 OCR \u2192 translate", Toast.LENGTH_SHORT).show()
        }
    }
}
