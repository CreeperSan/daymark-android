package com.creepersan.memorialday.activity.index

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.widget.composable.CSTextTitle
import com.creepersan.memorialday.widget.composable.token.CSTheme

class IndexActivity : BaseComposeActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CSTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { padding ->
                    CSTextTitle("首页")
                }
            }
        }
    }


}