package com.creepersan.memorialday.module.route.navigator

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CSNavigatorBusinessRouteTest {

    @Test
    fun businessRouteArgumentsRoundTripWithoutStringlyTypedCallers() {
        val expected = CSBusinessPageEnter(
            destination = CSBusinessDestination.TimelineEventEditor,
            anniversaryId = 8L,
            timelineEventId = 13L,
            categoryId = "memory"
        )

        val actual = CSNavigator.businessPageEnter(
            Intent().putExtras(CSNavigator.businessPageArguments(expected))
        )

        assertEquals(expected, actual)
    }

    @Test
    fun missingBusinessArgumentsFallBackToSafeHomeEntry() {
        val actual = CSNavigator.businessPageEnter(Intent())

        assertEquals(CSBusinessDestination.Home, actual.destination)
        assertNull(actual.anniversaryId)
        assertNull(actual.timelineEventId)
        assertNull(actual.categoryId)
    }

    @Test
    fun unavailableResultRetainsTypedContext() {
        val result = CSNavigator.businessPageResult(
            outcome = CSPageOutcome.Unavailable,
            anniversaryId = 8L,
            timelineEventId = 13L,
            categoryId = "memory"
        )

        assertEquals(CSPageOutcome.Unavailable, result.outcome)
        assertEquals(8L, result.data.getLong("cs_business_anniversary_id"))
        assertEquals(13L, result.data.getLong("cs_business_timeline_event_id"))
        assertEquals("memory", result.data.getString("cs_business_category_id"))
    }

    @Test
    fun closingBusinessActivityTwiceIsSafe() {
        ActivityScenario.launch(CSBusinessActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                CSNavigator.close(activity)
                CSNavigator.close(activity)
            }
        }
    }

    @Test
    fun disposedObserverDoesNotReceiveBusinessPageResult() {
        ActivityScenario.launch(CSBusinessActivity::class.java).use { scenario ->
            lateinit var observer: io.reactivex.rxjava3.observers.TestObserver<CSBusinessPageExit>
            scenario.onActivity { activity ->
                observer = CSNavigator.toSettings(activity).test()
                observer.dispose()
            }

            observer.assertNoValues()
        }
    }

    @Test
    fun destroyingHostCancelsPendingBusinessNavigation() {
        val scenario = ActivityScenario.launch(CSBusinessActivity::class.java)
        lateinit var observer: io.reactivex.rxjava3.observers.TestObserver<CSBusinessPageExit>
        scenario.onActivity { activity ->
            observer = CSNavigator.toSettings(activity).test()
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)

        observer.assertValue { it.outcome == CSPageOutcome.Cancelled }
    }
}
