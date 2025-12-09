package com.example.crimetracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crimetracking.data.Crime
import com.example.crimetracking.data.CrimePdfScraper
import com.example.crimetracking.ui.CrimeReportAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportsFragment : Fragment() {

    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText = view.findViewById(R.id.statusText)

        CoroutineScope(Dispatchers.Main).launch {
            statusText.text = "Loading reports…"

            val reports = withContext(Dispatchers.IO) {
                CrimePdfScraper.fetchCrimes(requireContext())
            }

            if (reports.isEmpty()) {
                statusText.text = "No reports found."
                return@launch
            }

            // Convert list to big readable text
            val displayText = reports.joinToString("\n\n") {
                "• ${it.offense}"
            }

            statusText.text = displayText
        }
    }
}
