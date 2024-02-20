package com.example.digiteqentrytask

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import com.example.digiteqentrytask.databinding.BubbleLayoutBinding
import kotlin.math.roundToInt

const val BUBBLE_START_POSITION_Y_PERCENTAGE = 0.33

class BubbleService : Service() {

    private lateinit var binding: BubbleLayoutBinding
    private lateinit var windowManager: WindowManager
    private var portraitWidthPx = 0
    private var portraitHeightPx = 0
    private var landscapeWidthPx = 0
    private var landscapeHeightPx = 0

    private var lastAction = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        Log.d(BubbleService::class.java.simpleName, "onCreate")
        val bubbleStartPositionY =
            (BUBBLE_START_POSITION_Y_PERCENTAGE * this.resources.configuration.screenHeightDp.dpToPx(
                this
            )).roundToInt()
        Log.d(BubbleService::class.java.simpleName, "start pos y: $bubbleStartPositionY")

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            portraitHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            portraitWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
        } else {
            landscapeHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            landscapeWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
        }

        binding = BubbleLayoutBinding.inflate(LayoutInflater.from(this))
        val view = binding.root

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
            .apply {// set initial position of the bubble, if gravity is
                // used x and y are and offset from that position in direction of screen space
                // if gravity is TOP END x goes left and y down
                gravity = Gravity.TOP or Gravity.END
                x = 0
                y = bubbleStartPositionY
            }

        // update view position based on touch input
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {


                    //remember the initial position.
                    initialX = params.x
                    initialY = params.y

                    //remember the initial touch location
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    lastAction = event.action
                    true
                }

                MotionEvent.ACTION_UP -> {
                    // Identify click action
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        //Open MainActivity on bubble click
                        startActivity(
                            Intent(this@BubbleService, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        )
                        //close the service and remove the bubble
                        stopSelf()
                    }
                    lastAction = event.action
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    //Calculate the X and Y coordinates of the view.
                    //Computes relative to WindowManager.LayoutParams.gravity. If it is END
                    // we have to use subtraction.
                    //In case of START we have to use addition
                    params.x = (initialX - (event.rawX - initialTouchX).toInt()).coerceAtLeast(0)
                    //Computes relative to WindowManager.LayoutParams.gravity. If it is BOTTOM
                    // we have to use subtraction.
                    //In case of TOP we have to use addition
                    params.y = (initialY + (event.rawY - initialTouchY)).toInt().coerceAtLeast(0)
                    Log.d(BubbleService::class.java.simpleName, "${params.x}, ${params.y}")
                    //Update the layout with new X & Y coordinate
                    windowManager.updateViewLayout(view, params)
                    lastAction = event.action
                    true
                }

                else -> false
            }
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(binding.root)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && (portraitWidthPx == 0 || portraitHeightPx == 0)) {
            portraitWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            portraitHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
        }

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && (landscapeWidthPx == 0 || landscapeHeightPx == 0)) {
            landscapeWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            landscapeHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
        }

        val orientationString = if (newConfig.orientation == 1) "portrait" else "landscape"
        Log.d(BubbleService::class.java.simpleName, orientationString)
        Log.d(
            BubbleService::class.java.simpleName,
            "config width: ${newConfig.screenWidthDp}, height: ${newConfig.screenHeightDp}"
        )
        Log.d(
            BubbleService::class.java.simpleName,
            "config width: ${newConfig.screenWidthDp.dpToPx(this)}, height: ${newConfig.screenHeightDp.dpToPx(this)}"
        )
        Log.d(
            BubbleService::class.java.simpleName,
            "portrait width: ${portraitWidthPx}, height: ${portraitHeightPx}"
        )
        Log.d(
            BubbleService::class.java.simpleName,
            "landscape width: ${landscapeWidthPx}, height: ${landscapeHeightPx}"
        )

        params.x = computeNewPositionInPxByPercentage(
            params.x,
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) landscapeWidthPx else portraitWidthPx,
            newConfig.screenWidthDp.dpToPx(this),
        )

        params.y = computeNewPositionInPxByPercentage(
            params.y,
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) landscapeHeightPx else portraitHeightPx,
            newConfig.screenHeightDp.dpToPx(this),
        )

        Log.d(
            BubbleService::class.java.simpleName,
            "computed params x: ${params.x}, y: ${params.y}"
        )
        windowManager.updateViewLayout(binding.root, params)
    }
}

fun Float.pxToDp(context: Context): Int =
    (this / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()

fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).roundToInt()

fun computeNewPositionInPxByPercentage(
    coordinate: Int,
    originalScreenSize: Int,
    newScreenSize: Int,
): Int {
    val originalPositionXPercentageToScreen = coordinate.toFloat() / originalScreenSize
    val finalPositionX = originalPositionXPercentageToScreen * newScreenSize
    return finalPositionX.roundToInt()
}