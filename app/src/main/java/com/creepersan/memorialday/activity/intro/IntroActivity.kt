package com.creepersan.memorialday.activity.intro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.creepersan.memorialday.base.activity.BaseComposeActivity

class IntroActivity : BaseComposeActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { BuildPage() }
    }


    @Preview
    @Composable
    fun BuildPage() {

    }



    @Preview
    @Composable
    fun BuildPager() {

    }

    @Preview
    @Composable
    fun BuildButtonGroup() {

    }


}