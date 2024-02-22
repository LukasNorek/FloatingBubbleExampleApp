package com.example.digiteqentrytask

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import org.junit.Rule
import org.junit.Test


class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun checkOverlayPermission_permissionNotGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // There is no way to programmatically set the permission for tests. This assert will never
        // fail. And there is no way to test positive path for the permission.
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

        // Setting should be shown and when back button is pressed we should be back in the app.
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.launch_bubble_button))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }
}