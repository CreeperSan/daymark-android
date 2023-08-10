package com.creepersan.memorialday.activity

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import com.creepersan.memorialday.activity.add.AddEditMemorialDayActivity

object MemorialDayRoute {

    fun toAddMemorialDayPage(context: Activity): Intent {
        val intent = Intent(context, AddEditMemorialDayActivity::class.java)
        intent.putExtra(AddEditMemorialDayActivity.INTENT_TYPE, AddEditMemorialDayActivity.INTENT_TYPE_ADD)
        return intent
    }

    fun toEditMemorialDayPage(context: Activity, id: Long): Intent {
        val intent = Intent(context, AddEditMemorialDayActivity::class.java)
        intent.putExtra(AddEditMemorialDayActivity.INTENT_TYPE, AddEditMemorialDayActivity.INTENT_TYPE_EDIT)
        intent.putExtra(AddEditMemorialDayActivity.INTENT_MEMORIAL_DAY_ID, id)
        return intent
    }

    fun finish(context: Context) {
        when (context) {
            is Activity -> {
                context.finish()
            }
            is Fragment -> {
                context.activity?.finish()
            }
        }
    }

}