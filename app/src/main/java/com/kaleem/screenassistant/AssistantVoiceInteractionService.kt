package com.kaleem.screenassistant

import android.service.voice.VoiceInteractionService

/**
 * Registers BitAssist as a real Android assistant (not just an ACTION_ASSIST
 * activity). Once picked as the default assistant, the system triggers
 * AssistantSessionService -> AssistantSession on every assist invocation and
 * — critically — hands the session a screenshot it captured itself, with no
 * MediaProjection/cast permission dialog. No logic needed here; the system
 * drives the whole flow via the session.
 */
class AssistantVoiceInteractionService : VoiceInteractionService()
