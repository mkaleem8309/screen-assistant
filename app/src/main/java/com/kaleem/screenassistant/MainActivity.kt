package com.kaleem.screenassistant

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        val startBtn: Button = findViewById(R.id.btn_start)
        val assistantBtn: Button = findViewById(R.id.btn_set_assistant)

        startBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                launchOverlayService()
            }
        }

        assistantBtn.setOnClickListener {
            // Opens the system "Assist & voice input" screen where the user picks
            // BitAssist as their Default assistant app. There's no API to set this
            // programmatically — Android requires the explicit user step.
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            statusText.text = "Overlay permission granted. Tap to start."
        }
    }

    private fun launchOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        statusText.text = "Running. Swipe up from the bottom-right corner in any app."
    }
}
