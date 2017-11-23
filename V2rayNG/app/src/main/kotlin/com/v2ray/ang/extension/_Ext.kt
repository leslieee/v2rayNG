package com.v2ray.ang.extension

import android.content.Context
import com.v2ray.ang.AngApplication
import me.dozen.dpreference.DPreference
import org.json.JSONObject

/**
 * Some extensions
 */

val Context.v2RayApplication: AngApplication
    get() = applicationContext as AngApplication

val Context.defaultDPreference: DPreference
    get() = v2RayApplication.defaultDPreference


fun JSONObject.putOpt(pair: Pair<String, Any>) = putOpt(pair.first, pair.second)!!
fun JSONObject.putOpt(pairs: Map<String, Any>) = pairs.forEach { putOpt(it.key to it.value) }
