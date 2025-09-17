package com.l906009.smarttransport

import android.Manifest
import android.content.Context
import com.l906009.smarttransport.R
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent


class MainActivity : ComponentActivity() {
    private lateinit var map: MapView
    private val btnZoomIn: MaterialButton by lazy { findViewById(R.id.btnZoomIn) }
    private val btnZoomOut: MaterialButton by lazy { findViewById(R.id.btnZoomOut) }
    private val btnLocation: MaterialButton by lazy { findViewById(R.id.btnLocation) }
    private var locationOverlay: MyLocationNewOverlay? = null
    private var geo_ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val cacheDir = cacheDir
        val osmdroidBasePath = File(cacheDir, "osmdroid")
        val osmdroidTileCache = File(osmdroidBasePath, "tile")

        osmdroidBasePath.mkdirs()
        osmdroidTileCache.mkdirs()

        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.osmdroidBasePath = osmdroidBasePath
        config.osmdroidTileCache = osmdroidTileCache

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mapCreate()
        btnCreate()
        userLocation()

        val stationLoader = StationLoader(applicationContext, map)
        stationLoader.loadStations()

        val transLoader = TransLoader(applicationContext, map)
        transLoader.loadTrans()
    }

    private fun mapCreate() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)


        map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

        val last = getSharedPreferences("last_pos", Context.MODE_PRIVATE)
        val lastZoom = last.getFloat("last_zoom", 15f)
        val lastLat = last.getFloat("last_lat", 54.811939f)
        val lastLon = last.getFloat("last_lon", 56.061237f)
        map.controller.setZoom(lastZoom.toDouble())
        map.controller.setCenter(GeoPoint(lastLat.toDouble(), lastLon.toDouble()))

        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMultiTouchControls(true)
        map.isClickable = true

        map.minZoomLevel = 5.0
        map.maxZoomLevel = 20.0
        map.addMapListener(object : MapListener {
            private var lastUpdateTime = 0L
            private val UPDATE_DELAY = 1000L
            private fun savePosition() {
                val prefs = getSharedPreferences("last_pos", Context.MODE_PRIVATE)
                prefs.edit {
                    putFloat("last_zoom", map.zoomLevelDouble.toFloat())
                    val center = map.mapCenter as GeoPoint
                    putFloat("last_lat", center.latitude.toFloat())
                    putFloat("last_lon", center.longitude.toFloat())
                }
            }
            override fun onScroll(event: ScrollEvent?): Boolean {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime > UPDATE_DELAY) {
                    savePosition()
                    val stationLoader = StationLoader(applicationContext, map)
                    stationLoader.loadFromCache()
                }
                lastUpdateTime = System.currentTimeMillis()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime > UPDATE_DELAY) {
                    savePosition()
                    val stationLoader = StationLoader(applicationContext, map)
                    stationLoader.loadFromCache()
                }
                lastUpdateTime = System.currentTimeMillis()
                return false
            }
        })
        map.invalidate()
    }
    private fun btnCreate() {
        btnZoomIn.setOnClickListener {
            if (map.zoomLevelDouble < map.maxZoomLevel){
                map.controller.zoomIn()
            }
        }
        btnZoomOut.setOnClickListener {
            if (map.zoomLevelDouble > map.minZoomLevel){
                map.controller.zoomOut()
            }
        }
        btnLocation.setOnClickListener {
            geo_ready = !geo_ready
            if (geo_ready) btnLocation.icon = ContextCompat.getDrawable(this, R.drawable.baseline_my_location_24) else btnLocation.icon = ContextCompat.getDrawable(this, R.drawable.baseline_location_searching_24)
            userLocation()
        }
    }

    private fun userLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 100)
            return
        }
        val provider = GpsMyLocationProvider(this)

        provider.clearLocationSources()
        if (geo_ready) {
            provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
            provider.addLocationSource(LocationManager.GPS_PROVIDER);
        } else {
            locationOverlay?.disableMyLocation()
            provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
        }
        val sizeInDp = 18
        val density = resources.displayMetrics.density
        val px = (sizeInDp * density).toInt()
        val bitmap = AppCompatResources.getDrawable(this, R.drawable.location_marker)?.toBitmap(px, px)
        locationOverlay?.let {
            map.overlays.remove(it)
        }
        locationOverlay = MyLocationNewOverlay(provider, map).apply {
            setPersonIcon(bitmap)
            enableMyLocation()
            enableFollowLocation()
            setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        map.overlays.add(locationOverlay)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (geo_ready) locationOverlay?.enableMyLocation()
        val prefs = getSharedPreferences("last_pos", Context.MODE_PRIVATE)
        val lastLat = prefs.getFloat("last_lat", 54.811939f)
        val lastLon = prefs.getFloat("last_lon", 56.061237f)
        val lastZoom = prefs.getFloat("last_zoom", 15f)
        map.controller.setZoom(lastZoom.toDouble())
        map.controller.setCenter(GeoPoint(lastLat.toDouble(), lastLon.toDouble()))
        val stationLoader = StationLoader(applicationContext, map)
        stationLoader.loadFromCache()
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("last_pos", Context.MODE_PRIVATE)
        prefs.edit {
            putFloat("last_zoom", map.zoomLevelDouble.toFloat())
            val center = map.mapCenter as GeoPoint
            putFloat("last_lat", center.latitude.toFloat())
            putFloat("last_lon", center.longitude.toFloat())
        }
        locationOverlay?.disableMyLocation()
        map.onPause()
    }
}
