package com.kaleem.screenassistant

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * The minimal "what can I do" sheet shown after assistant invocation.
 * "Search screen" now actually captures the screen (via CaptureService)
 * and shows it full-bleed to prove the pipeline works. Region selection,
 * OCR, and the real search/translate actions are still to come — the
 * captured bitmap isn't used for anything yet.
 */
class AssistantActivity : AppCompatActivity() {

    private lateinit var capturedPreview: ImageView

    private val captureReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val bitmap = CaptureHolder.latestBitmap ?: return
            capturedPreview.setImageBitmap(bitmap)
            capturedPreview.visibility = View.VISIBLE
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, CaptureService::class.java)
                .putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(CaptureService.EXTRA_RESULT_DATA, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        capturedPreview = findViewById(R.id.captured_preview)
        capturedPreview.setOnClickListener { finish() }

        findViewById<View>(R.id.root_scrim).setOnClickListener { finish() }
        // Prevent taps on the card itself from bubbling to the scrim and closing it.
        findViewById<View>(R.id.assistant_card).setOnClickListener { }

        findViewById<TextView>(R.id.btn_search_screen).setOnClickListener {
            requestScreenCapture()
        }

        findViewById<TextView>(R.id.btn_translate).setOnClickListener {
            Toast.makeText(this, "TODO: capture region \u2192 OCR \u2192 translate", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(CaptureService.ACTION_CAPTURE_READY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(captureReadyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(captureReadyReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(captureReadyReceiver)
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
    }
}
