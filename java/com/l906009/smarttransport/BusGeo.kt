package com.l906009.smarttransport

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi @Serializable
data class BusGeo(
    val u_id: String?,
    val tt_id: Int?,
    val tt_title: String?,
    val tt_title_en: String?,
    val pk_id: Int?,
    val u_statenum: String?,
    val u_garagnum: Int?,
    val u_model: String?,
    val u_timenav: String?,
    val u_lat: Double,
    val u_long: Double,
    val u_speed: Int?,
    val u_course: Int?,
    val u_inv: Int?,
    val mr_id: Int?,
    val mr_num: String?,
    val rl_racetype: String?,
    val pk_title: String?,
    val pk_title_en: String?,
    val rl_firststation_title: String?,
    val rl_firststation_title_en: String?,
    val rl_laststation_title: String?,
    val rl_laststation_title_en: String?
)
