package com.creepersan.memorialday.business.network.prefab

import com.google.gson.annotations.SerializedName

data class HttpResponse<T> (
    val code: Int,
    val msg: String,
    val data: T?,
) {

    fun success()  = code == 200

}