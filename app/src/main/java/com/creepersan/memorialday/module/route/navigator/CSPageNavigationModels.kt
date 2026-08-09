package com.creepersan.memorialday.module.route.navigator

import android.os.Bundle

enum class CSPageOutcome {
    Completed,
    Cancelled,
    Failed,
    Unavailable
}

data class CSPageExitModel(
    val outcome: CSPageOutcome,
    val data: Bundle = Bundle()
)

data class CSLoginPageEnter(
    val account: String = ""
)

data class CSLoginPageExit(
    val outcome: CSPageOutcome,
    val account: String? = null
)

data class CSRegisterPageEnter(
    val account: String = "",
    val email: String = ""
)

data class CSRegisterPageExit(
    val outcome: CSPageOutcome,
    val account: String? = null,
    val email: String? = null
)

data class CSEmailVerificationPageEnter(
    val account: String,
    val email: String,
    val expiresAtMillis: Long = 0L
)

data class CSEmailVerificationPageExit(
    val outcome: CSPageOutcome,
    val code: String? = null
)

data class CSForgotPasswordPageEnter(
    val email: String = ""
)

data class CSForgotPasswordPageExit(
    val outcome: CSPageOutcome
)

enum class CSBusinessDestination {
    Home,
    AnniversaryEditor,
    AnniversaryDetail,
    Settings,
    CategoryManagement,
    ReminderManagement,
    Timeline,
    TimelineEventEditor,
    MarkEditor,
    PermissionExplanation,
    VersionSwitch,
    CategoryEditor
}

data class CSBusinessPageEnter(
    val destination: CSBusinessDestination,
    val anniversaryId: Long? = null,
    val timelineEventId: Long? = null,
    val categoryId: String? = null
)

data class CSBusinessPageExit(
    val outcome: CSPageOutcome,
    val anniversaryId: Long? = null,
    val timelineEventId: Long? = null,
    val categoryId: String? = null
)
