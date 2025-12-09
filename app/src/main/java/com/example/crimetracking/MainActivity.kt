package com.example.crimetracking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.data.geojson.GeoJsonLayer
import org.json.JSONObject
import java.io.InputStream
import java.util.Scanner
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.maps.android.PolyUtil
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import com.example.crimetracking.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val client = OkHttpClient()
    private var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private val fallbackOrigin = LatLng(43.0384, -76.1343)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializeMapFeatures() {
        if (map != null) return

        // Initialize map fragment and set callback
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Bottom Sheet initialization
        val bottomSheet = findViewById<View>(R.id.crime_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 120
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Place SDK initialization
        val apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        setupAutocomplete()
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        // Check if user is logged in. If not, redirect to LoginActivity.
        if (user == null || !user.isEmailVerified) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            initializeMapFeatures()
        }
    }

    // Helper function to fetch and draw route
    private fun fetchAndDrawRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        apiKey: String
    ) {
        val origin = "$originLat,$originLng"
        val destination = "$destLat,$destLng"

        // Construct URL for the Directions API call
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin&" +
                "destination=$destination&" +
                "mode=walking&" +
                "key=$apiKey"

        // Use coroutines to run network request on a background thread
        GlobalScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")

                    val jsonResponse = response.body?.string() ?: return@launch
                    val polyline = parsePolylineFromDirectionsJson(jsonResponse)

                    // Switch back to the Main thread to draw on the UI (the map)
                    launch(Dispatchers.Main) {
                        drawRouteOnMap(polyline)
                    }
                }
            } catch (e: Exception) {
                Log.e("Routing", "Error fetching route: ${e.message}", e)
            }
        }
    }

    // Helper function to extract the Polyline string from the JSON response
    private fun parsePolylineFromDirectionsJson(jsonResponse: String): String {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val overviewPolyline = route.getJSONObject("overview_polyline")
                return overviewPolyline.getString("points")
            }
        } catch (e: Exception) {
            Log.e("Routing", "Error parsing JSON", e)
        }
        return ""
    }

    // Helper function to decode and draw the route on map
    private fun drawRouteOnMap(encodedPolyline: String) {
        if (encodedPolyline.isEmpty() || map == null) return

        val points = PolyUtil.decode(encodedPolyline)

        val polylineOptions = com.google.android.gms.maps.model.PolylineOptions()
            .addAll(points)
            .width(15f)
            .color(0xFF0000FF.toInt())
            .geodesic(true)

        routePolyline = map?.addPolyline(polylineOptions)
        val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
        for (point in points) {
            bounds.include(point)
        }
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    // Stores the current route line
    private var routePolyline: com.google.android.gms.maps.model.Polyline? = null

    private fun setupAutocomplete() {
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.LAT_LNG, Place.Field.NAME))
        autocompleteFragment.setActivityMode(AutocompleteActivityMode.OVERLAY)

        autocompleteFragment.setOnPlaceSelectedListener(object : com.google.android.libraries.places.widget.listener.PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val destinationLatLng = place.latLng ?: return
                routePolyline?.remove()

                // Origin of Syracuse University
                val originLat = 43.0384
                val originLng = -76.1343
                val apiKey = BuildConfig.MAPS_API_KEY

                // Route calculation to the user's selected destination
                fetchAndDrawRoute(
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destinationLatLng.latitude,
                    destLng = destinationLatLng.longitude,
                    apiKey = apiKey
                )
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("Places", "An error occurred: $status")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val syracuseUniversity = LatLng(43.0384, -76.1343)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(syracuseUniversity, 10f))
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        googleMap.uiSettings.isZoomControlsEnabled = true

        try {
            // Enable the built-in "My Location" blue dot
            googleMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.e("Map", "My Location feature requires permission: ${e.message}")
        }

        requestCurrentLocation()
        addCrimeMarkers(googleMap)
    }

    // Checks for permission and requests location
    private fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        // Permission is granted, get the last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    Log.d("Location", "Current location fetched: $currentLocation")
                    // Move map camera to user's location
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                } else {
                    Log.w("Location", "Last known location is null, using fallback.")
                    currentLocation = fallbackOrigin
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Error getting location: ${e.message}", e)
                currentLocation = fallbackOrigin
            }
    }

    // Handle result of permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, try to get location again
            requestCurrentLocation()
        } else {
            // Permission denied, use fallback
            currentLocation = fallbackOrigin
            Log.w("Location", "Location permission denied, using fallback.")
        }
    }

    private fun addCrimeMarkers(googleMap: GoogleMap) {
        try {
            val jsonObject = readJsonResource(R.raw.syr_crime_data)
            val layer = GeoJsonLayer(googleMap, jsonObject)

            // Style markers
            for (feature in layer.features) {
                val address = feature.getProperty("ADDRESS") ?: "Unknown Location"
                val crimeType = feature.getProperty("CODE_DEFINED") ?: "Unknown Crime"

                // Stores raw data in snippet for the InfoWindowAdapter to use
                val rawDate = feature.getProperty("DATEEND") ?: ""
                val rawTime = feature.getProperty("TIMESTART") ?: ""
                val style = com.google.maps.android.data.geojson.GeoJsonPointStyle()
                style.title = address

                // Packs data into snippet for the bubble: "Type|Date|Time"
                style.snippet = "$crimeType|$rawDate|$rawTime"
                style.icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                feature.pointStyle = style
            }

            // Add layer to map
            layer.addLayerToMap()

            // Use the layer's listener
            layer.setOnFeatureClickListener { feature ->
                // Extract raw properties directly from the feature
                val crimeType = feature.getProperty("CODE_DEFINED") ?: "Unknown Crime"
                val address = feature.getProperty("ADDRESS") ?: "Unknown Location"
                val rawDate = feature.getProperty("DATEEND") ?: ""
                val rawTime = feature.getProperty("TIMESTART") ?: ""

                // Format the data
                val formattedDate = formatDate(rawDate)
                val formattedTime = formatTime(rawTime)

                // Update the Bottom Sheet
                updateBottomSheet(crimeType, address, formattedDate, formattedTime)
            }

        } catch (e: Exception) {
            Log.e("CrimeTracker", "Error loading GeoJSON data", e)
        }
    }

    private fun updateBottomSheet(type: String, address: String, date: String, time: String) {
        findViewById<TextView>(R.id.crime_title).text = type
        findViewById<TextView>(R.id.crime_address).text = address
        findViewById<TextView>(R.id.crime_date).text = "Date: $date"
        findViewById<TextView>(R.id.crime_time).text = "Time: $time"

        // Slide the sheet up
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Helper Functions for Formatting

    // Parses "Thu, 02 Jan 2025..." to "01/02/2025"
    private fun formatDate(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

            val date = inputFormat.parse(rawDate)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            rawDate
        }
    }

    // Parses "1806" (Integer) to "06:06 PM"
    private fun formatTime(rawTime: String): String {
        return try {
            val paddedTime = rawTime.padStart(4, '0')

            val inputFormat = SimpleDateFormat("HHmm", Locale.US)
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.US)

            val date = inputFormat.parse(paddedTime)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            rawTime
        }
    }

    // Helper function to read the raw JSON file into a JSONObject
    private fun readJsonResource(resourceId: Int): JSONObject {
        val inputStream: InputStream = resources.openRawResource(resourceId)
        val scanner = Scanner(inputStream).useDelimiter("\\A")
        val jsonString = if (scanner.hasNext()) scanner.next() else ""
        return JSONObject(jsonString)
    }
}

class CustomInfoWindowAdapter(private val context: AppCompatActivity) : GoogleMap.InfoWindowAdapter {
    override fun getInfoWindow(marker: Marker): View {
        val view = context.layoutInflater.inflate(R.layout.custom_info_window, null)

        val titleView = view.findViewById<TextView>(R.id.tvTitle)
        val snippetView = view.findViewById<TextView>(R.id.tvSnippet)

        val parts = marker.snippet?.split("|") ?: listOf("", "")
        val crimeType = parts.getOrNull(0) ?: ""

        titleView.text = marker.title
        snippetView.text = crimeType

        return view
    }

    override fun getInfoContents(marker: Marker): View? = null
}
