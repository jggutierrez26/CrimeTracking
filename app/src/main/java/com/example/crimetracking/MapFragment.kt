package com.example.crimetracking

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.crimetracking.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.view.Gravity
import android.widget.Toast
import com.google.maps.android.data.geojson.GeoJsonFeature

class MapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val client = OkHttpClient()
    private var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private val fallbackOrigin = LatLng(43.0384, -76.1343)
    private var routePolyline: com.google.android.gms.maps.model.Polyline? = null
    private var crimeFeatures: List<GeoJsonFeature> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        initializeMapFeatures()

        val headerView = binding.root.findViewById<View>(R.id.bottom_sheet_header)

        headerView.post {
            val headerHeightPx = headerView.height
            bottomSheetBehavior.peekHeight = headerHeightPx
            map?.setPadding(0, 0, 0, headerHeightPx)
        }
    }

    private fun initializeMapFeatures() {
        if (map != null) return

        // Initialize map fragment and set callback
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Bottom Sheet initialization
        val bottomSheet = binding.root.findViewById<View>(R.id.crime_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        //bottomSheetBehavior.peekHeight = 80
        bottomSheetBehavior.peekHeight = 1
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Place SDK initialization
        val apiKey = requireContext().getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }
        setupAutocomplete()
    }

    private fun fetchAndDrawRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        apiKey: String
    ) {
        val origin = "$originLat,$originLng"
        val destination = "$destLat,$destLng"

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin&" +
                "destination=$destination&" +
                "mode=walking&" +
                "key=$apiKey"

        GlobalScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")

                    val jsonResponse = response.body?.string() ?: return@launch
                    val polyline = parsePolylineFromDirectionsJson(jsonResponse)

                    launch(Dispatchers.Main) {
                        drawRouteOnMap(polyline)
                    }
                }
            } catch (e: Exception) {
                Log.e("Routing", "Error fetching route: ${e.message}", e)
            }
        }
    }

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
        calculateCrimesAlongRoute(points)
    }

    private fun setupAutocomplete() {
        val autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.LAT_LNG, Place.Field.NAME))
        autocompleteFragment.setActivityMode(com.google.android.libraries.places.widget.model.AutocompleteActivityMode.OVERLAY)

        autocompleteFragment.setOnPlaceSelectedListener(object : com.google.android.libraries.places.widget.listener.PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val destinationLatLng = place.latLng ?: return
                routePolyline?.remove()

                val originLat = 43.0384
                val originLng = -76.1343
                val apiKey = requireContext().getString(R.string.google_maps_key)

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
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(requireActivity()))
        googleMap.uiSettings.isZoomControlsEnabled = true

        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.e("Map", "My Location feature requires permission: ${e.message}")
        }

        requestCurrentLocation()
        addCrimeMarkers(googleMap)
    }

    private fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    Log.d("Location", "Current location fetched: $currentLocation")
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

    private fun addCrimeMarkers(googleMap: GoogleMap) {
        try {
            val jsonObject = readJsonResource(R.raw.syr_crime_data)
            val layer = GeoJsonLayer(googleMap, jsonObject)
            crimeFeatures = layer.features.toList()

            for (feature in crimeFeatures) {
                val address = feature.getProperty("ADDRESS") ?: "Unknown Location"
                val crimeType = feature.getProperty("CODE_DEFINED") ?: "Unknown Crime"

                val rawDate = feature.getProperty("DATEEND") ?: ""
                val rawTime = feature.getProperty("TIMESTART") ?: ""
                val style = com.google.maps.android.data.geojson.GeoJsonPointStyle()
                style.title = address

                style.snippet = "$crimeType|$rawDate|$rawTime"
                style.icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                feature.pointStyle = style
            }

            layer.addLayerToMap()

            layer.setOnFeatureClickListener { feature ->
                val crimeType = feature.getProperty("CODE_DEFINED") ?: "Unknown Crime"
                val address = feature.getProperty("ADDRESS") ?: "Unknown Location"
                val rawDate = feature.getProperty("DATEEND") ?: ""
                val rawTime = feature.getProperty("TIMESTART") ?: ""

                val formattedDate = formatDate(rawDate)
                val formattedTime = formatTime(rawTime)

                updateBottomSheet(crimeType, address, formattedDate, formattedTime)
            }

        } catch (e: Exception) {
            Log.e("CrimeTracker", "Error loading GeoJSON data", e)
        }
    }

    private fun calculateCrimesAlongRoute(routePoints: List<LatLng>) {
        if (routePoints.isEmpty() || crimeFeatures.isEmpty()) return

        val oneMileInMeters = 1609.34
        val crimesWithinRadius = mutableSetOf<GeoJsonFeature>()

        for (feature in crimeFeatures) {
            val geometry = feature.geometry
            if (geometry is com.google.maps.android.data.geojson.GeoJsonPoint) {
                val crimeLocation = geometry.coordinates

                // Check if crime location is within 1 mile of the route polyline.
                if (PolyUtil.isLocationOnPath(crimeLocation, routePoints, true, oneMileInMeters)) {
                    crimesWithinRadius.add(feature)
                }
            }
        }

        val crimeCount = crimesWithinRadius.size

        // Display the result using a long toast message
        val message = "There are $crimeCount crime incidents along the way."
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
            show()
        }
    }

    private fun updateBottomSheet(type: String, address: String, date: String, time: String) {
        binding.root.findViewById<TextView>(R.id.crime_title).text = type
        binding.root.findViewById<TextView>(R.id.crime_address).text = address
        binding.root.findViewById<TextView>(R.id.crime_date).text = "Date: $date"
        binding.root.findViewById<TextView>(R.id.crime_time).text = "Time: $time"

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

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

    private fun readJsonResource(resourceId: Int): JSONObject {
        val inputStream: InputStream = resources.openRawResource(resourceId)
        val scanner = Scanner(inputStream).useDelimiter("\\A")
        val jsonString = if (scanner.hasNext()) scanner.next() else ""
        return JSONObject(jsonString)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

