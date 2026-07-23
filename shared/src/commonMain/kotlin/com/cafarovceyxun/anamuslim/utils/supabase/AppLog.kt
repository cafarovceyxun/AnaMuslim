package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppLog(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("type")
    val type: String,
    @SerialName("stack_trace")
    val stack_trace: String,
    @SerialName("device_info")
    val device_info: String,
    @SerialName("app_version")
    val app_version: String,
    @SerialName("place")
    val place: String? = null,
    @SerialName("created_at")
    val created_at: String? = null
)
