package com.example.floatingbubbleexampleapp

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
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.example.floatingbubbleexampleapp.databinding.BubbleLayoutBinding
import kotlin.math.roundToInt


const val BUBBLE_START_POSITION_Y_PERCENTAGE = 0.33
const val SPRING_ANIMATION_VELOCITY = 5000f

class BubbleService : Service() {

    private lateinit var binding: BubbleLayoutBinding
    private lateinit var windowManager: WindowManager
    private var portraitWidthPx = 0
    private var portraitHeightPx = 0
    private var portraitScreenMiddle = 0
    private var landscapeWidthPx = 0
    private var landscapeHeightPx = 0
    private var landscapeScreenMiddle = 0

    private var lastAction = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var params: WindowManager.LayoutParams
    private var bubbleSizeInPx: Float = 0f
    private var halfOfBubbleSizeInPx: Int = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        binding = BubbleLayoutBinding.inflate(LayoutInflater.from(this))
        val view = binding.root
        Log.d(BubbleService::class.java.simpleName, "onCreate")

        bubbleSizeInPx = this.resources.getDimension(R.dimen.bubble_size)
        halfOfBubbleSizeInPx = bubbleSizeInPx.div(2).roundToInt()

        val bubbleStartPositionY =
            (BUBBLE_START_POSITION_Y_PERCENTAGE * this.resources.configuration.screenHeightDp.dpToPx(
                this
            )).roundToInt()

        Log.d(BubbleService::class.java.simpleName, "start pos y: $bubbleStartPositionY")

        // Initialize values used for computing screen orientation changes.
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            portraitHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            portraitWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            portraitScreenMiddle = portraitWidthPx.floorDiv(2) - halfOfBubbleSizeInPx
        } else {
            landscapeHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            landscapeWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            landscapeScreenMiddle = landscapeWidthPx.floorDiv(2) - halfOfBubbleSizeInPx
        }

        // Initialize stick to edge animation
        val animationX = SpringAnimation(FloatValueHolder())
        animationX.addUpdateListener { _, value, _ ->
            params.x = value.toInt()

            // check if view is added to window manager and update it
            if (view.parent != null) {
                windowManager.updateViewLayout(view, params)
            }
        }

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
            .apply {// Set initial position of the bubble. If gravity is
                // used x and y are an offset from that position in direction of screen space
                // If gravity is TOP END x goes left and y down.
                gravity = Gravity.TOP or Gravity.END
                x = 0
                y = bubbleStartPositionY
            }

        // Update view position based on touch input
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    //Remember the initial position.
                    initialX = params.x
                    initialY = params.y

                    //Remember the initial touch location
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
                    } else {
                        animationX.setStartValue(params.x.toFloat())

                        val springForce = let {
                            animationX.setStartVelocity(SPRING_ANIMATION_VELOCITY)
                            val finalPosition: Float =
                                if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && params.x > portraitScreenMiddle) {
                                    portraitWidthPx.toFloat()
                                } else if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && params.x > landscapeScreenMiddle) {
                                    landscapeWidthPx.toFloat()
                                } else {
                                    animationX.setStartVelocity(-SPRING_ANIMATION_VELOCITY)
                                    0f // Right side of the screen
                                }
                            SpringForce(finalPosition)
                        }.apply {
                            stiffness = SpringForce.STIFFNESS_VERY_LOW
                            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                        }

                        animationX.setSpring(springForce)
                        animationX.start()
                    }
                    lastAction = event.action
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Stop the animation when manually moving the bubble. Otherwise the animation causes delay in bubble movement.
                    animationX.cancel()
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

        // If first orientation was landscape initialize values for portrait.
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && (portraitWidthPx == 0 || portraitHeightPx == 0)) {
            portraitWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            portraitHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            portraitScreenMiddle = portraitWidthPx.floorDiv(2) - halfOfBubbleSizeInPx
        }
        // If first orientation was portrait initialize values for landscape.
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && (landscapeWidthPx == 0 || landscapeHeightPx == 0)) {
            landscapeWidthPx = this.resources.configuration.screenWidthDp.dpToPx(this)
            landscapeHeightPx = this.resources.configuration.screenHeightDp.dpToPx(this)
            landscapeScreenMiddle = landscapeWidthPx.floorDiv(2) - halfOfBubbleSizeInPx
        }

        params.x = computeNewPositionInPxByPreviousOrientationRatio(
            coordinate = params.x,
            originalScreenSize = if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) landscapeWidthPx else portraitWidthPx,
            newScreenSize = newConfig.screenWidthDp.dpToPx(this),
        )

        params.y = computeNewPositionInPxByPreviousOrientationRatio(
            coordinate = params.y,
            originalScreenSize = if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) landscapeHeightPx else portraitHeightPx,
            newScreenSize = newConfig.screenHeightDp.dpToPx(this),
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

fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).roundToInt()

fun computeNewPositionInPxByPreviousOrientationRatio(
    coordinate: Int,
    originalScreenSize: Int,
    newScreenSize: Int,
): Int {
    val originalPositionXPercentageToScreen = coordinate.toFloat() / originalScreenSize
    val finalPositionX = originalPositionXPercentageToScreen * newScreenSize
    return finalPositionX.roundToInt()
}