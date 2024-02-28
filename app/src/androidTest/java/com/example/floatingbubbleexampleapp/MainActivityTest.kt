package com.example.floatingbubbleexampleapp

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun checkOverlayPermission_permissionNotGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // This assert will never fail as the app is installed fresh each time and the permission is not set
        Settings.canDrawOverlays(ApplicationProvider.getApplicationContext())
            .let { canDrawOverlays ->
                if (canDrawOverlays) {
                    Assert.fail("Overlay permission is already granted")
                }
            }
        // Check that dialog is shown when first opening the app.
        Espresso.onView(ViewMatchers.withText(context.resources.getString(R.string.permission_dialog_title)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Click on negative action and check that dialog is not shown.
        Espresso.onView(ViewMatchers.withText(context.resources.getString(R.string.negative_action)))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText(context.resources.getString(R.string.permission_dialog_title)))
            .check(ViewAssertions.doesNotExist())

        // Click on launch bubble button.
        Espresso.onView(ViewMatchers.withId(R.id.launch_bubble_button))
            .perform(ViewActions.click())

        // Check that dialog is shown
        Espresso.onView(ViewMatchers.withText(context.resources.getString(R.string.permission_dialog_title)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Press positive button
        Espresso.onView(ViewMatchers.withText(context.resources.getString(R.string.positive_action)))
            .perform(ViewActions.click())


        // Settings should be shown and when back button is pressed we should be back in the app.
        // works on english language devices
        allowPermission()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()

        val bubbleButton = Espresso.onView(ViewMatchers.withId(R.id.launch_bubble_button))
        // Check that MainActivity with bubble button is displayed and click on the button
        bubbleButton.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        bubbleButton.perform(ViewActions.click())

        // Check if BubbleService is running
        assertTrue(context.isServiceRunning(BubbleService::class.java))
    }


    private fun allowPermission() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val allowButtonText = "Allow display over other apps"
        uiDevice.waitForIdle()
        val allowButton = uiDevice.findObject(UiSelector().text(allowButtonText))
        if (allowButton.exists()) {
            allowButton.click()
        } else {
            val digiteqAppButton =
                uiDevice.findObject(UiSelector().textContains("DigiteqEntryTask"))
            if (digiteqAppButton.exists()) {
                digiteqAppButton.click()
                uiDevice.findObject(UiSelector().text(allowButtonText)).click()
                uiDevice.pressBack()
            } else {
                Assert.fail("Overlay permission not granted")
            }
        }
        uiDevice.pressBack()
    }


    @Suppress("DEPRECATION") // Deprecated for third party Services.
    private fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == service.name }

}