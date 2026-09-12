package com.kaleem.screenassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/**
 * Keeps a small, invisible touch-catcher pinned to the bottom-right corner of
 * the screen. A swipe that starts in that corner and moves up past a
 * threshold launches AssistantActivity — this is the "bluff" stand-in for a
 * real assistant-invocation gesture.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var touchStrip: View? = null

    private var downY = 0f
    private val swipeThresholdPx by lazy { (72 * resources.displayMetrics.density) }

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIF_ID = 1001

        // Size of the invisible corner touch target, in dp.
        private const val STRIP_WIDTH_DP = 70
        private const val STRIP_HEIGHT_DP = 90
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addTouchStrip()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        touchStrip?.let { runCatching { windowManager.removeView(it) } }
        touchStrip = null
    }

    private fun addTouchStrip() {
        val density = resources.displayMetrics.density
        val widthPx = (STRIP_WIDTH_DP * density).toInt()
        val heightPx = (STRIP_HEIGHT_DP * density).toInt()

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 0
            y = 0
        }

        val strip = View(this).apply {
            // Fully invisible; only here to catch the gesture.
            alpha = 0f
            setOnTouchListener { _, event ->
                handleTouch(event)
            }
        }

        windowManager.addView(strip, params)
        touchStrip = strip
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = downY - event.rawY
                if (deltaY > swipeThresholdPx && abs(deltaY) > abs(event.rawX)) {
                    launchAssistant()
                    return true
                }
            }
        }
        return true
    }

    private fun launchAssistant() {
        val intent = Intent(this, AssistantActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Corner-gesture assistant active")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}
