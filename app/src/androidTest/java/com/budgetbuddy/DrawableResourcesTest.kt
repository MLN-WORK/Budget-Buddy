package com.budgetbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
/*
 * Start of class
 * Name of class and related classes (parent/child classes): DrawableResourcesTest
 * Parent class: Any; child classes: none; related classes: DrawableResources, JUnit, and the application code under test.
 * What the class does: Verifies the DrawableResources behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class DrawableResourcesTest {
    @Test
    fun receiptPlaceholderIsARealDrawable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context.getDrawable(R.drawable.placeholder_image))
    }
}
// End of class: DrawableResourcesTest
