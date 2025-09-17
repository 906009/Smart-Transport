package com.l906009.smarttransport

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker

class TransLoader(
    private val context: Context,
    private val map: MapView,
    private val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_bus),
    private val serverUrl: String = "ws://192.168.88.254:2727/transport"
) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("trans_cache", Context.MODE_PRIVATE)
    private val Trans_overlay = FolderOverlay().apply {
        name = "Stations"
    }
    init {
        map.overlays.add(Trans_overlay)
    }

    @OptIn(InternalSerializationApi::class)
    fun loadTrans(){
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("{\"lat_min\": 0.0,\"lat_max\": 200.0,\"lon_min\": 0.0,\"lon_max\": 200.0}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text != null){
                    loadJSON(text)
                }
            }

            private fun loadJSON(text: String) {
                val units: List<BusGeo> = Json.decodeFromString(text)
                map.post {
                    Trans_overlay.items.clear()
                    for (unit in units) {
                        addTransMarker(unit.u_id, unit.tt_title, unit.u_model, unit.u_lat, unit.u_long)
                    }
                    map.invalidate()
                }
            }

            private fun addTransMarker(
                id: String?,
                title: String?,
                model: String?,
                lat: Double,
                lon: Double
            ) {
                if (id != null){
                    val marker = Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        this.title = title + "\n" + id + "\n" + model
                        this.icon = this@TransLoader.icon
                        isEnabled = map.zoomLevelDouble > 15.0
                    }
                    Trans_overlay.add(marker)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket connection closed: $reason (Code: $code)")
            }
        }
        val webSocket = client.newWebSocket(request, webSocketListener)
        webSocket
    }
}
