package com.bertschell.immichcontacts.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import com.yalantis.ucrop.UCropActivity

/**
 * UCropActivity with system back-gesture exclusion zones on both screen edges.
 * Without this, dragging the crop frame near the screen edge triggers the
 * Android back gesture and closes the cropper.
 */
class GestureSafeUCropActivity : UCropActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply exclusion after the view tree is laid out (rects need final sizes)
        findViewById<View>(android.R.id.content)?.viewTreeObserver?.addOnGlobalLayoutListener {
            val scale = resources.displayMetrics.density
            val w = (32 * scale).toInt()
            val content = findViewById<View>(android.R.id.content) ?: return@addOnGlobalLayoutListener
            val rects = listOf(
                Rect(0, 0, w, content.height),
                Rect(content.width - w, 0, content.width, content.height),
            )
            content.systemGestureExclusionRects = rects
        }
    }
}
