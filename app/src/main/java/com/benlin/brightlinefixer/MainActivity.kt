package com.benlin.brightlinefixer

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_CODE = 100
        private var overlayView: BrightnessOverlayView? = null
        private var windowManager: WindowManager? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 檢查覆蓋權限
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")), REQUEST_CODE
            )
            return
        }

        setupUI()
    }

    private fun setupUI() {
        brightnessSlider.addOnChangeListener { _, value, _ ->
            overlayView?.setMaskAlpha((255 * (value / 100)).toInt())
        }

        startOverlayButton.setOnClickListener {
            val position = linePositionEdit.text.toString().toIntOrNull() ?: 100
            val width = lineWidthEdit.text.toString().toIntOrNull() ?: 50
            
            createBrightnessOverlay(position, width)
            Toast.makeText(this, "亮線隱藏已啟動", Toast.LENGTH_SHORT).show()
        }

        stopOverlayButton.setOnClickListener {
            removeBrightnessOverlay()
            Toast.makeText(this, "亮線隱藏已停止", Toast.LENGTH_SHORT).show()
        }

        val previewMask = findViewById<DraggableMaskView>(R.id.maskPreview)
        
        // 滑桿控制透明度
        brightnessSlider.addOnChangeListener { _, value, _ ->
            val alpha = (255 * (value / 100)).toInt()
            previewMask.setMaskAlpha(alpha)
            overlayView?.setMaskAlpha(alpha)
        }
        
        // 拖曳位置同步
        previewMask.setOnPositionChangedListener { position ->
            linePositionEdit.setText(position.toString())
            overlayView?.setBrightLineRect(Rect(position, 0, position + 50, 9999))
        }
        
        applyButton.setOnClickListener {
            val position = previewMask.x.toInt()
            createBrightnessOverlay(position, 50)
        }
    }

    @SuppressLint("WrongConstant")
    private fun createBrightnessOverlay(position: Int, width: Int) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayView = BrightnessOverlayView(this).apply {
            setBrightLineRect(Rect(position, 0, position + width, 9999))
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        
        windowManager?.addView(overlayView, params)
    }

    private fun removeBrightnessOverlay() {
        try {
            windowManager?.removeView(overlayView)
            overlayView = null
        } catch (e: Exception) {
            // View可能已移除
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                setupUI()
            } else {
                Toast.makeText(this, "需要「顯示在其他應用程式上層」權限", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBrightnessOverlay()
    }
}

class BrightnessOverlayView(context: Context) : View(context) {
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 0, 0, 0)
    }
    private lateinit var brightLineRect: Rect
    private var maskAlpha = 120

    fun setBrightLineRect(rect: Rect) {
        brightLineRect = rect
        invalidate()
    }

    fun setMaskAlpha(alpha: Int) {
        maskAlpha = alpha
        maskPaint.alpha = alpha
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        maskPaint.alpha = maskAlpha
        canvas.drawRect(brightLineRect, maskPaint)
    }
}
