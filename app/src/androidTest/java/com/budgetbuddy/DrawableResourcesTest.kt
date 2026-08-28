package com.budgetbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawableResourcesTest {
    @Test
    fun receiptPlaceholderIsARealDrawable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context.getDrawable(R.drawable.placeholder_image))
    }
}
