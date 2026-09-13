package com.kaleem.screenassistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Sole purpose: a shortcut to the system screen where the user picks BitAssist
 * as their default assistant. Android has no API to set this programmatically,
 * so this just jumps to the picker — no other logic lives here.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val assistantBtn: Button = findViewById(R.id.btn_set_assistant)
        assistantBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        }
    }
}
