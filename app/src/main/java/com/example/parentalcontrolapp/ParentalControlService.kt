package com.example.parentalcontrolapp

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import android.util.Log

class ParentalControlService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false

    private val blockedPackages = setOf(
        "com.google.android.youtube",
        "com.instagram.android"
    )

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Anti-tamper: Block unauthorized settings changes targeting our app
        if (packageName == "com.android.settings" && !SecurityManager.isParentAuthenticated) {
            if (isSettingsTargetingOurApp()) {
                Log.w("ParentalControlService", "Anti-tamper: Intercepted Settings access targeting ParentalControlApp")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d("ParentalControlService", "Foreground package: $packageName")

            if (blockedPackages.contains(packageName)) {
                showBlockingOverlay(packageName)
            } else if (packageName != this.packageName && packageName != "com.android.settings") {
                // Remove overlay if we switch to another app (excluding settings, since settings might be blocked/backed out)
                removeBlockingOverlay()
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ParentalControlService", "Service Interrupted")
    }

    private fun isSettingsTargetingOurApp(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        
        val appNameMatches = rootNode.findAccessibilityNodeInfosByText("ParentalControlApp")
        val shieldMatches = rootNode.findAccessibilityNodeInfosByText("Guardian Shield")
        
        val targetFound = !appNameMatches.isNullOrEmpty() || !shieldMatches.isNullOrEmpty()
        
        appNameMatches?.forEach { it.recycle() }
        shieldMatches?.forEach { it.recycle() }
        rootNode.recycle()
        
        return targetFound
    }

    private fun showBlockingOverlay(blockedPackage: String) {
        if (isOverlayShowing) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.layout_blocking_overlay, null)

        val appNameTextView = view.findViewById<TextView>(R.id.appNameTextView)
        val goBackButton = view.findViewById<Button>(R.id.goBackButton)

        val appName = when (blockedPackage) {
            "com.google.android.youtube" -> "YouTube"
            "com.instagram.android" -> "Instagram"
            else -> "Blocked App"
        }
        appNameTextView.text = "$appName is blocked by your parents."

        goBackButton.setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_HOME)
            removeBlockingOverlay()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        try {
            windowManager?.addView(view, params)
            overlayView = view
            isOverlayShowing = true
        } catch (e: Exception) {
            Log.e("ParentalControlService", "Error showing overlay: ${e.message}", e)
        }
    }

    private fun removeBlockingOverlay() {
        if (!isOverlayShowing || overlayView == null) return

        try {
            windowManager?.removeView(overlayView)
        } catch (e: Exception) {
            Log.e("ParentalControlService", "Error removing overlay: ${e.message}", e)
        } finally {
            overlayView = null
            isOverlayShowing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBlockingOverlay()
    }
}
