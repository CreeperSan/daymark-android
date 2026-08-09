package com.creepersan.memorialday.module.route.navigator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class CSNavigationResultActivity : ComponentActivity() {

    private val destinationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        CSNavigator.deliverResult(
            navigationId = intent.getStringExtra(EXTRA_NAVIGATION_ID),
            resultCode = result.resultCode,
            data = result.data
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            return
        }
        val targetClassName = intent.getStringExtra(EXTRA_TARGET_CLASS)
        if (targetClassName.isNullOrBlank()) {
            CSNavigator.deliverFailure(intent.getStringExtra(EXTRA_NAVIGATION_ID))
            finish()
            return
        }
        val destination = try {
            Intent(this, Class.forName(targetClassName)).putExtras(
                intent.getBundleExtra(EXTRA_DESTINATION_ARGUMENTS) ?: Bundle()
            )
        } catch (_: ClassNotFoundException) {
            CSNavigator.deliverFailure(intent.getStringExtra(EXTRA_NAVIGATION_ID))
            finish()
            return
        }
        destinationLauncher.launch(destination)
    }

    companion object {
        const val EXTRA_NAVIGATION_ID = "cs_navigation_id"
        const val EXTRA_TARGET_CLASS = "cs_navigation_target_class"
        const val EXTRA_DESTINATION_ARGUMENTS = "cs_navigation_destination_arguments"
    }
}
