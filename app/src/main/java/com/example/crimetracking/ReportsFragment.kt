package com.example.crimetracking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private lateinit var rvCrimeReports: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var crimeReportAdapter: CrimeReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        initializeViews(view)
        setupRecyclerView()
        loadCrimeReports()

        return view
    }

    private fun initializeViews(view: View) {
        rvCrimeReports = view.findViewById(R.id.rvCrimeReports)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        crimeReportAdapter = CrimeReportAdapter()
        rvCrimeReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = crimeReportAdapter
        }
    }

    private fun loadCrimeReports() {
        progressBar.visibility = View.VISIBLE
        rvCrimeReports.visibility = View.GONE
        emptyState.visibility = View.GONE

        try {
            val jsonObject = readJsonResource(R.raw.syr_crime_data)
            val crimeReports = parseCrimeData(jsonObject)

            progressBar.visibility = View.GONE

            if (crimeReports.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rvCrimeReports.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                rvCrimeReports.visibility = View.VISIBLE
                crimeReportAdapter.submitList(crimeReports)
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error loading crime data", e)
            progressBar.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            rvCrimeReports.visibility = View.GONE
        }
    }

    private fun parseCrimeData(jsonObject: JSONObject): List<CrimeReport> {
        val crimeReports = mutableListOf<CrimeReport>()

        try {
            // Parse GeoJSON features directly
            val features = jsonObject.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")

                val address = properties.optString("ADDRESS", "Unknown Location")
                val crimeType = properties.optString("CODE_DEFINED", "Unknown Crime")
                val rawDate = properties.optString("DATEEND", "")
                val rawTime = properties.optString("TIMESTART", "")

                val formattedDate = formatDate(rawDate)
                val formattedTime = formatTime(rawTime)

                crimeReports.add(
                    CrimeReport(
                        address = address,
                        type = crimeType,
                        date = formattedDate,
                        time = formattedTime,
                        rawDate = rawDate,
                        rawTime = rawTime
                    )
                )
            }

            // Sort by date (most recent first)
            crimeReports.sortByDescending { it.rawDate }

        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error parsing crime data", e)
        }

        return crimeReports
    }

    private fun formatDate(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

            val date = inputFormat.parse(rawDate)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            rawDate.take(10).ifEmpty { "Unknown" }
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
            rawTime.ifEmpty { "Unknown" }
        }
    }

    private fun readJsonResource(resourceId: Int): JSONObject {
        val inputStream: InputStream = resources.openRawResource(resourceId)
        val scanner = Scanner(inputStream).useDelimiter("\\A")
        val jsonString = if (scanner.hasNext()) scanner.next() else ""
        return JSONObject(jsonString)
    }
}


