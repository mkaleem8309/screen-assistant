package com.kaleem.screenassistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Not a feature — required glue. Android's assistant picker silently
 * excludes any VoiceInteractionService that doesn't also declare a
 * resolvable RecognitionService (an intentional, documented Google API
 * coupling: https://github.com/home-assistant/android/discussions/5974).
 * We don't do speech recognition — voice is explicitly out of scope for
 * this app — so every callback here just reports "unsupported."
 */
class AssistantRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback?) {
        // No-op: nothing to cancel, we never start listening.
    }

    override fun onStopListening(listener: Callback?) {
        // No-op: nothing to stop, we never start listening.
    }
}
