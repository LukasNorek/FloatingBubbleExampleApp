package com.example.digiteqentrytask

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import com.example.digiteqentrytask.databinding.BubbleLayoutBinding

class BubbleService : Service() {

    private lateinit var binding: BubbleLayoutBinding
    private lateinit var windowManager: WindowManager

    private var lastAction = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        binding = BubbleLayoutBinding.inflate(LayoutInflater.from(this))
        val view = binding.root

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
            .apply {// set initial position of the bubble
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
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
                        startActivity(Intent(this@BubbleService, MainActivity::class.java))
                        //close the service and remove the bubble
                        stopSelf()
                    }
                    lastAction = event.action
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    //Calculate the X and Y coordinates of the view.
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()

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

}