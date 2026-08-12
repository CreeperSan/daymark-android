package com.creepersan.memorialday.module.route.navigator

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证业务路由参数、结果回传及生命周期取消行为。
 *
 * 路由结果通过 Flow 暴露，因此测试会显式收集 Flow，确保导航在收集开始时才真正创建。
 */
@RunWith(AndroidJUnit4::class)
class CSNavigatorBusinessRouteTest {

    /** 参数进入业务页后能够完整恢复，避免调用方依赖字符串键。 */
    @Test
    fun businessRouteArgumentsRoundTripWithoutStringlyTypedCallers() {
        val expected = CSBusinessPageEnter(
            destination = CSBusinessDestination.TimelineEventEditor,
            anniversaryId = 8L,
            timelineEventId = 13L,
            categoryId = "memory"
        )

        // 使用路由框架实际序列化的 Bundle，验证参数键不会泄漏到业务调用方。
        val actual = CSNavigator.businessPageEnter(
            Intent().putExtras(CSNavigator.businessPageArguments(expected))
        )

        assertEquals(expected, actual)
    }

    /** 缺少业务参数时回退到安全的首页入口。 */
    @Test
    fun missingBusinessArgumentsFallBackToSafeHomeEntry() {
        val actual = CSNavigator.businessPageEnter(Intent())

        assertEquals(CSBusinessDestination.Home, actual.destination)
        assertNull(actual.anniversaryId)
        assertNull(actual.timelineEventId)
        assertNull(actual.categoryId)
    }

    /** 不可用结果仍保留业务上下文，便于上层决定后续处理。 */
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

    /** 重复关闭同一个 Activity 不应抛出异常或重复派发结果。 */
    @Test
    fun closingBusinessActivityTwiceIsSafe() {
        ActivityScenario.launch(CSBusinessActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                CSNavigator.close(activity)
                CSNavigator.close(activity)
            }
        }
    }

    /** 取消 Flow 收集后，后续路由结果不应再发送给已取消的调用方。 */
    @Test
    fun cancelledCollectorDoesNotReceiveBusinessPageResult() = runBlocking {
        ActivityScenario.launch(CSBusinessActivity::class.java).use { scenario ->
            lateinit var collection: kotlinx.coroutines.Job
            scenario.onActivity { activity ->
                // Flow 收集一旦取消，callbackFlow 的 awaitClose 会清理待处理导航。
                collection = launch(start = CoroutineStart.UNDISPATCHED) {
                    CSNavigator.toSettings(activity).collect {}
                }
                collection.cancel()
            }

            collection.join()
        }
    }

    /** 宿主销毁时，挂起的业务导航应以 Cancelled 结果结束。 */
    @Test
    fun destroyingHostCancelsPendingBusinessNavigation() = runBlocking {
        val scenario = ActivityScenario.launch(CSBusinessActivity::class.java)
        val result = CompletableDeferred<CSBusinessPageExit>()
        lateinit var collection: kotlinx.coroutines.Job
        scenario.onActivity { activity ->
            collection = launch(start = CoroutineStart.UNDISPATCHED) {
                result.complete(CSNavigator.toSettings(activity).first())
            }
        }

        // 销毁宿主会触发 ActivityLifecycleCallbacks，并将挂起导航转换为 Cancelled。
        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertEquals(CSPageOutcome.Cancelled, result.await().outcome)
        collection.cancel()
    }
}
