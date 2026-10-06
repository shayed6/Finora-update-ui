package com.example

import android.content.Context
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetInflationTest {

    @Test
    fun testWidgetRemoteViewsInflation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
        try {
            val inflated = views.apply(context, null)
            println("Inflation success: $inflated")
        } catch (e: Throwable) {
            println("=== WIDGET INFLATION EXCEPTION START ===")
            e.printStackTrace()
            println("=== WIDGET INFLATION EXCEPTION END ===")
            throw e
        }
    }
}
