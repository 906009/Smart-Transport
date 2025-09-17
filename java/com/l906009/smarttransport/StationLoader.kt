package com.l906009.smarttransport

import com.l906009.smarttransport.R
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import java.io.IOException


class StationLoader(
    private val context: Context,
    private val map: MapView,
    private val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_directions_bus_24),
    private val serverUrl: String = "http://192.168.88.254:2727/stations"
) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("stations_cache", Context.MODE_PRIVATE)
    private val Stations_overlay = FolderOverlay().apply {
        name = "Stations"
    }
    init {
        map.overlays.add(Stations_overlay)
    }


    fun loadStations() {
        loadFromCache()
        val hashRequest = Request.Builder()
            .url("$serverUrl/hash")
            .build()

        client.newCall(hashRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Snackbar.make(map, "Нет подключения к интернету", Snackbar.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val newHash = response.body?.string()?.trim()
                    val savedHash = prefs.getString("hash", null)

                    if (newHash != null && newHash != savedHash) {
                        loadFromServer(newHash)
                    } else {
                        loadFromCache()
                    }
                } else {
                    loadFromCache()
                }
            }
        })
    }

    fun loadFromCache() {
        val json = prefs.getString("stations", null)
        if (json != null) {
            loadJSON(json)
        }
    }

    fun loadFromServer(newHash: String) {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                prefs.edit {
                    putString("hash", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    prefs.edit {
                        putString("hash", newHash)
                        putString("stations", json)
                    }
                    map.overlays.removeAll { it is Marker }
                    loadJSON(json)
                }
            }
        })
    }

    private fun loadJSON(json: String?) {
        if (json != null) {
            val jsonArray = JSONArray(json)
            map.post {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString(("st_id"))
                    val title = obj.getString("st_title")
                    val lat = obj.getString("st_lat").toDouble()
                    val lon = obj.getString("st_long").toDouble()
                    addStationMarker(id, title, lat, lon)
                }
            }
            map.invalidate()
        }
    }

    private fun addStationMarker(id: String, title: String, lat: Double, lon: Double) {
        val marker = Marker(map).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title + "\n" + id
            this.icon = this@StationLoader.icon
            isEnabled = map.zoomLevelDouble > 15.0
        }
        Stations_overlay.add(marker)
    }
}