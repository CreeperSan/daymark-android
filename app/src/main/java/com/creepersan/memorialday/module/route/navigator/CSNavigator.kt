package com.creepersan.memorialday.module.route.navigator

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.creepersan.memorialday.activity.add.AddEditMemorialDayActivity
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import com.creepersan.memorialday.activity.forget_password.ForgetPasswordActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.register.RegisterActivity
import io.reactivex.rxjava3.core.Observable
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CSNavigator {

    private const val EXTRA_OUTCOME = "cs_navigation_outcome"
    private const val EXTRA_RESULT_DATA = "cs_navigation_result_data"
    private const val EXTRA_LOGIN_ACCOUNT = "cs_login_account"
    private const val EXTRA_REGISTER_ACCOUNT = "cs_register_account"
    private const val EXTRA_REGISTER_EMAIL = "cs_register_email"
    private const val EXTRA_VERIFICATION_ACCOUNT = "cs_verification_account"
    private const val EXTRA_VERIFICATION_EMAIL = "cs_verification_email"
    private const val EXTRA_VERIFICATION_EXPIRES_AT = "cs_verification_expires_at"
    private const val EXTRA_VERIFICATION_CODE = "cs_verification_code"
    private const val EXTRA_FORGOT_PASSWORD_EMAIL = "cs_forgot_password_email"
    private const val EXTRA_BUSINESS_DESTINATION = "cs_business_destination"
    private const val EXTRA_BUSINESS_ANNIVERSARY_ID = "cs_business_anniversary_id"
    private const val EXTRA_BUSINESS_TIMELINE_EVENT_ID = "cs_business_timeline_event_id"
    private const val EXTRA_BUSINESS_CATEGORY_ID = "cs_business_category_id"

    private val pendingNavigations = ConcurrentHashMap<String, PendingNavigation>()

    fun toLogin(
        activity: Activity,
        params: CSLoginPageEnter = CSLoginPageEnter()
    ): Observable<CSLoginPageExit> = open(
        activity = activity,
        targetClass = LoginActivity::class.java,
        arguments = Bundle().apply { putString(EXTRA_LOGIN_ACCOUNT, params.account) }
    ).map { exit ->
        CSLoginPageExit(
            outcome = exit.outcome,
            account = exit.data.getString(EXTRA_LOGIN_ACCOUNT)
        )
    }

    fun toRegister(
        activity: Activity,
        params: CSRegisterPageEnter = CSRegisterPageEnter()
    ): Observable<CSRegisterPageExit> = open(
        activity = activity,
        targetClass = RegisterActivity::class.java,
        arguments = Bundle().apply {
            putString(EXTRA_REGISTER_ACCOUNT, params.account)
            putString(EXTRA_REGISTER_EMAIL, params.email)
        }
    ).map { exit ->
        CSRegisterPageExit(
            outcome = exit.outcome,
            account = exit.data.getString(EXTRA_REGISTER_ACCOUNT),
            email = exit.data.getString(EXTRA_REGISTER_EMAIL)
        )
    }

    fun toEmailVerification(
        activity: Activity,
        params: CSEmailVerificationPageEnter
    ): Observable<CSEmailVerificationPageExit> = open(
        activity = activity,
        targetClass = CSEmailVerificationActivity::class.java,
        arguments = Bundle().apply {
            putString(EXTRA_VERIFICATION_ACCOUNT, params.account)
            putString(EXTRA_VERIFICATION_EMAIL, params.email)
            putLong(EXTRA_VERIFICATION_EXPIRES_AT, params.expiresAtMillis)
        }
    ).map { exit ->
        CSEmailVerificationPageExit(
            outcome = exit.outcome,
            code = exit.data.getString(EXTRA_VERIFICATION_CODE)
        )
    }

    fun toForgotPassword(
        activity: Activity,
        params: CSForgotPasswordPageEnter = CSForgotPasswordPageEnter()
    ): Observable<CSForgotPasswordPageExit> = open(
        activity = activity,
        targetClass = ForgetPasswordActivity::class.java,
        arguments = Bundle().apply { putString(EXTRA_FORGOT_PASSWORD_EMAIL, params.email) }
    ).map { exit -> CSForgotPasswordPageExit(outcome = exit.outcome) }

    fun toAddEditPage(
        activity: Activity,
        memorialDayId: Long? = null
    ): Observable<CSPageExitModel> {
        val arguments = Bundle().apply {
            putString(
                AddEditMemorialDayActivity.INTENT_TYPE,
                if (memorialDayId == null) {
                    AddEditMemorialDayActivity.INTENT_TYPE_ADD
                } else {
                    AddEditMemorialDayActivity.INTENT_TYPE_EDIT
                }
            )
            memorialDayId?.let {
                putLong(AddEditMemorialDayActivity.INTENT_MEMORIAL_DAY_ID, it)
            }
        }
        return open(activity, AddEditMemorialDayActivity::class.java, arguments)
    }

    fun toHome(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.Home))

    fun toAnniversaryEditor(
        activity: Activity,
        anniversaryId: Long? = null
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(CSBusinessDestination.AnniversaryEditor, anniversaryId = anniversaryId)
    )

    fun toAnniversaryDetail(
        activity: Activity,
        anniversaryId: Long
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(CSBusinessDestination.AnniversaryDetail, anniversaryId = anniversaryId)
    )

    fun toSettings(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.Settings))

    fun toCategoryManagement(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.CategoryManagement))

    fun toReminderManagement(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.ReminderManagement))

    fun toTimeline(
        activity: Activity,
        anniversaryId: Long
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(CSBusinessDestination.Timeline, anniversaryId = anniversaryId)
    )

    fun toTimelineEventEditor(
        activity: Activity,
        anniversaryId: Long,
        timelineEventId: Long? = null
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(
            destination = CSBusinessDestination.TimelineEventEditor,
            anniversaryId = anniversaryId,
            timelineEventId = timelineEventId
        )
    )

    fun toMarkEditor(
        activity: Activity,
        anniversaryId: Long
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(CSBusinessDestination.MarkEditor, anniversaryId = anniversaryId)
    )

    fun toPermissionExplanation(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.PermissionExplanation))

    fun toVersionSwitch(activity: Activity): Observable<CSBusinessPageExit> =
        toBusinessPage(activity, CSBusinessPageEnter(CSBusinessDestination.VersionSwitch))

    fun toCategoryEditor(
        activity: Activity,
        categoryId: String? = null
    ): Observable<CSBusinessPageExit> = toBusinessPage(
        activity,
        CSBusinessPageEnter(CSBusinessDestination.CategoryEditor, categoryId = categoryId)
    )

    fun close(
        activity: Activity,
        exit: CSPageExitModel = CSPageExitModel(CSPageOutcome.Cancelled)
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        activity.setResult(
            when (exit.outcome) {
                CSPageOutcome.Completed -> Activity.RESULT_OK
                CSPageOutcome.Cancelled,
                CSPageOutcome.Failed,
                CSPageOutcome.Unavailable -> Activity.RESULT_CANCELED
            },
            Intent()
                .putExtra(EXTRA_OUTCOME, exit.outcome.name)
                .putExtra(EXTRA_RESULT_DATA, exit.data)
        )
        activity.finish()
    }

    fun loginAccount(intent: Intent): String = intent.getStringExtra(EXTRA_LOGIN_ACCOUNT).orEmpty()

    fun registerAccount(intent: Intent): String = intent.getStringExtra(EXTRA_REGISTER_ACCOUNT).orEmpty()

    fun registerEmail(intent: Intent): String = intent.getStringExtra(EXTRA_REGISTER_EMAIL).orEmpty()

    fun verificationAccount(intent: Intent): String =
        intent.getStringExtra(EXTRA_VERIFICATION_ACCOUNT).orEmpty()

    fun verificationEmail(intent: Intent): String =
        intent.getStringExtra(EXTRA_VERIFICATION_EMAIL).orEmpty()

    fun verificationExpiresAt(intent: Intent): Long =
        intent.getLongExtra(EXTRA_VERIFICATION_EXPIRES_AT, 0L)

    fun forgotPasswordEmail(intent: Intent): String =
        intent.getStringExtra(EXTRA_FORGOT_PASSWORD_EMAIL).orEmpty()

    fun businessPageEnter(intent: Intent): CSBusinessPageEnter {
        val destination = intent.getStringExtra(EXTRA_BUSINESS_DESTINATION)
            ?.let { value -> CSBusinessDestination.entries.firstOrNull { it.name == value } }
            ?: CSBusinessDestination.Home
        return CSBusinessPageEnter(
            destination = destination,
            anniversaryId = intent.takeIf { it.hasExtra(EXTRA_BUSINESS_ANNIVERSARY_ID) }
                ?.getLongExtra(EXTRA_BUSINESS_ANNIVERSARY_ID, 0L)
                ?.takeIf { it > 0L },
            timelineEventId = intent.takeIf { it.hasExtra(EXTRA_BUSINESS_TIMELINE_EVENT_ID) }
                ?.getLongExtra(EXTRA_BUSINESS_TIMELINE_EVENT_ID, 0L)
                ?.takeIf { it > 0L },
            categoryId = intent.getStringExtra(EXTRA_BUSINESS_CATEGORY_ID)
        )
    }

    internal fun businessPageArguments(params: CSBusinessPageEnter): Bundle = Bundle().apply {
        putString(EXTRA_BUSINESS_DESTINATION, params.destination.name)
        params.anniversaryId?.let { putLong(EXTRA_BUSINESS_ANNIVERSARY_ID, it) }
        params.timelineEventId?.let { putLong(EXTRA_BUSINESS_TIMELINE_EVENT_ID, it) }
        params.categoryId?.let { putString(EXTRA_BUSINESS_CATEGORY_ID, it) }
    }

    fun verificationCodeResult(code: String): CSPageExitModel = CSPageExitModel(
        outcome = CSPageOutcome.Completed,
        data = Bundle().apply { putString(EXTRA_VERIFICATION_CODE, code) }
    )

    fun businessPageResult(
        outcome: CSPageOutcome,
        anniversaryId: Long? = null,
        timelineEventId: Long? = null,
        categoryId: String? = null
    ): CSPageExitModel = CSPageExitModel(
        outcome = outcome,
        data = Bundle().apply {
            anniversaryId?.let { putLong(EXTRA_BUSINESS_ANNIVERSARY_ID, it) }
            timelineEventId?.let { putLong(EXTRA_BUSINESS_TIMELINE_EVENT_ID, it) }
            categoryId?.let { putString(EXTRA_BUSINESS_CATEGORY_ID, it) }
        }
    )

    internal fun deliverResult(navigationId: String?, resultCode: Int, data: Intent?) {
        val exit = data?.getBundleExtra(EXTRA_RESULT_DATA)?.let { resultData ->
            val outcome = data.getStringExtra(EXTRA_OUTCOME)
                ?.let { name -> CSPageOutcome.entries.firstOrNull { it.name == name } }
                ?: if (resultCode == Activity.RESULT_OK) CSPageOutcome.Completed else CSPageOutcome.Cancelled
            CSPageExitModel(outcome = outcome, data = resultData)
        } ?: CSPageExitModel(
            outcome = if (resultCode == Activity.RESULT_OK) CSPageOutcome.Completed else CSPageOutcome.Cancelled
        )
        deliver(navigationId, exit)
    }

    internal fun deliverFailure(navigationId: String?) {
        deliver(navigationId, CSPageExitModel(CSPageOutcome.Failed))
    }

    private fun open(
        activity: Activity,
        targetClass: Class<out Activity>,
        arguments: Bundle
    ): Observable<CSPageExitModel> = Observable.create { emitter ->
        if (activity.isFinishing || activity.isDestroyed) {
            emitter.onNext(CSPageExitModel(CSPageOutcome.Cancelled))
            emitter.onComplete()
            return@create
        }

        val navigationId = UUID.randomUUID().toString()
        val pending = PendingNavigation(
            navigationId = navigationId,
            host = WeakReference(activity),
            emitter = emitter,
            onHostDestroyed = { id -> deliver(id, CSPageExitModel(CSPageOutcome.Cancelled)) }
        )
        pendingNavigations[navigationId] = pending
        emitter.setCancellable { remove(navigationId) }
        activity.application.registerActivityLifecycleCallbacks(pending)
        activity.startActivity(
            Intent(activity, CSNavigationResultActivity::class.java)
                .putExtra(CSNavigationResultActivity.EXTRA_NAVIGATION_ID, navigationId)
                .putExtra(CSNavigationResultActivity.EXTRA_TARGET_CLASS, targetClass.name)
                .putExtra(CSNavigationResultActivity.EXTRA_DESTINATION_ARGUMENTS, arguments)
        )
    }

    private fun toBusinessPage(
        activity: Activity,
        params: CSBusinessPageEnter
    ): Observable<CSBusinessPageExit> = open(
        activity = activity,
        targetClass = CSBusinessActivity::class.java,
        arguments = businessPageArguments(params)
    ).map { exit ->
        CSBusinessPageExit(
            outcome = exit.outcome,
            anniversaryId = exit.data.takeIf { it.containsKey(EXTRA_BUSINESS_ANNIVERSARY_ID) }
                ?.getLong(EXTRA_BUSINESS_ANNIVERSARY_ID),
            timelineEventId = exit.data.takeIf { it.containsKey(EXTRA_BUSINESS_TIMELINE_EVENT_ID) }
                ?.getLong(EXTRA_BUSINESS_TIMELINE_EVENT_ID),
            categoryId = exit.data.getString(EXTRA_BUSINESS_CATEGORY_ID)
        )
    }

    private fun deliver(navigationId: String?, exit: CSPageExitModel) {
        if (navigationId == null) {
            return
        }
        val pending = pendingNavigations.remove(navigationId) ?: return
        pending.unregister()
        if (!pending.emitter.isDisposed) {
            pending.emitter.onNext(exit)
            pending.emitter.onComplete()
        }
    }

    private fun remove(navigationId: String) {
        pendingNavigations.remove(navigationId)?.unregister()
    }

    private class PendingNavigation(
        private val navigationId: String,
        private val host: WeakReference<Activity>,
        val emitter: io.reactivex.rxjava3.core.ObservableEmitter<CSPageExitModel>,
        private val onHostDestroyed: (String) -> Unit
    ) : Application.ActivityLifecycleCallbacks {
        private var application: Application? = host.get()?.application

        override fun onActivityDestroyed(activity: Activity) {
            if (host.get() === activity) {
                onHostDestroyed(navigationId)
            }
        }

        fun unregister() {
            application?.unregisterActivityLifecycleCallbacks(this)
            application = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}