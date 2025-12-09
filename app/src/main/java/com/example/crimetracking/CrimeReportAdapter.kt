package com.example.crimetracking.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.crimetracking.R
import com.example.crimetracking.data.Crime

class CrimeReportAdapter(private val items: List<Crime>) :
    RecyclerView.Adapter<CrimeReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.dateText)
        val time: TextView = view.findViewById(R.id.timeText)
        val location: TextView = view.findViewById(R.id.locationText)
        val offense: TextView = view.findViewById(R.id.offenseText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crime_report, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = items[position]
        holder.date.text = report.date
        holder.time.text = report.time
        holder.location.text = report.location
        holder.offense.text = report.offense
    }
}
