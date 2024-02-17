package com.example.digiteqentrytask

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.digiteqentrytask.databinding.MainActivityLayoutBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityLayoutBinding

    private val startActivityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityLayoutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkOverlayPermission()
        setOpenBubbleButton()

    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            createPermissionDialog().show()
        }
    }

    private fun createPermissionDialog(): AlertDialog.Builder {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage("This app needs permission to draw over other app. Please allow this permission.")
            .setTitle("Permission")
            .setPositiveButton("Allow:") { dialog, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
                startActivityForResult.launch(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Decline") { dialog, _ ->
                dialog.cancel()
            }
        return builder
    }


    private fun setOpenBubbleButton() {
        binding.launchBubbleButton.setOnClickListener {
            checkOverlayPermission()
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this@MainActivity, BubbleService::class.java))
                finish()
            }
        }
    }
}