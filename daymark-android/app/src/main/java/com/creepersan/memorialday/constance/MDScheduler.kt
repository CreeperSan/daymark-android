package com.creepersan.memorialday.constance

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers


object MDScheduler {

    val database = Schedulers.io()

    val ui = AndroidSchedulers.mainThread()

}
