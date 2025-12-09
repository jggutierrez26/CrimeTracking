package com.example.crimetracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private lateinit var reportsRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsRecycler = view.findViewById(R.id.reportsRecycler)
        reportsRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Launch coroutine to fetch and display PDF data
        CoroutineScope(Dispatchers.Main).launch {
            val reports: List<Crime> = withContext(Dispatchers.IO) {
                CrimePdfScraper.fetchCrimeReports(requireContext())
            }

            reportsRecycler.adapter = CrimeReportAdapter(reports)
        }
    }
}
