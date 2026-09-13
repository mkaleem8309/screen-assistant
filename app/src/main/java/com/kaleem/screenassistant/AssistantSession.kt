package com.kaleem.screenassistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.service.voice.VoiceInteractionSession

/**
 * Receives the screenshot the system captured on our behalf (see the
 * SHOW_WITH_SCREENSHOT flag Android passes when it starts an assist session —
 * this is a per-user toggle under Settings > Assist app > "Use screenshot",
 * on by default). We stash it and hand off to AssistantActivity for the
 * actual UI, then hide the (invisible, content-less) session window.
 */
class AssistantSession(context: Context) : VoiceInteractionSession(context) {

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        CaptureHolder.latestBitmap = screenshot
        val intent = Intent(context, AssistantActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        hide()
    }
}
