import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.margsewa.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var currentPlatformView: AutoCompleteTextView
    private lateinit var destinationPlatformView: AutoCompleteTextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: GeoPoint? = null  // Save the user's current location here

    private val platformCoordinates = mapOf(
        "Platform 1" to GeoPoint(18.5752, 73.8526),
        "Platform 2" to GeoPoint(18.5753, 73.8527),
        "Platform 3" to GeoPoint(18.5754, 73.8528),
        "Platform 4" to GeoPoint(18.5755, 73.8529)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MargSewa)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overridePendingTransition(0, 0)

        initializeMap()

        setupSearchBar()

        findViewById<Button>(R.id.go_button).setOnClickListener {
            // Handle Go button click if needed
        }

        findViewById<Button>(R.id.directions_button).setOnClickListener {
            handleDirectionsButtonClick()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocationAndUpdateSearchBar()
    }

    private fun initializeMap() {
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        map = findViewById(R.id.map)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(18.0)
        val stationGeoPoint = GeoPoint(18.5751, 73.8526)  // Khadki station coordinates
        mapController.setCenter(stationGeoPoint)

        addPlatformMarkers()
    }

    private fun addPlatformMarkers() {
        platformCoordinates.forEach { (platform, geoPoint) ->
            val platformMarker = Marker(map)
            platformMarker.position = geoPoint
            platformMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            platformMarker.title = platform
            map.overlays.add(platformMarker)
        }
        map.invalidate()
    }

    private fun setupSearchBar() {
        val platformNames = platformCoordinates.keys.toTypedArray()
        val allOptions = arrayOf("Current Location") + platformNames  // Add "Current Location" to the list
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, allOptions)

        currentPlatformView = findViewById(R.id.current_platform)
        destinationPlatformView = findViewById(R.id.destination_platform)

        currentPlatformView.setAdapter(adapter)
        destinationPlatformView.setAdapter(adapter)
    }

    private fun handleDirectionsButtonClick() {
        val destinationPlatform = destinationPlatformView.text.toString()
        val endPoint = platformCoordinates[destinationPlatform]

        // Only proceed if a destination platform is selected and current location is available
        if (endPoint != null && currentLocation != null) {
            showRoute(currentLocation!!, endPoint)
        }
    }

    private fun showRoute(startPoint: GeoPoint, endPoint: GeoPoint) {
        map.overlays.removeIf { it is Polyline }

        val route = Polyline(map)
        route.addPoint(startPoint)
        route.addPoint(endPoint)
        route.color = android.graphics.Color.RED
        route.width = 5.0f
        map.overlays.add(route)

        map.invalidate()
    }

    private fun getCurrentLocationAndUpdateSearchBar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)

                showCurrentLocationMarker(currentLocation!!)

                updateSearchBarWithCurrentLocation(it.latitude, it.longitude)
            }
        }
    }

    private fun updateSearchBarWithCurrentLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val locationText = address.getAddressLine(0) ?: "Current Location"

            currentPlatformView.setText("Current Location")  // Directly set to "Current Location"
        } else {
            currentPlatformView.setText("Current Location")
        }
    }

    private fun showCurrentLocationMarker(location: GeoPoint) {
        val userLocationMarker = Marker(map)
        userLocationMarker.position = location
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userLocationMarker.title = "You are here"
        userLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.ic_current_location)  // Custom location icon
        map.overlays.add(userLocationMarker)

        map.controller.animateTo(location)
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocationAndUpdateSearchBar()
    }
}
