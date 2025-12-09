package com.example.crimetracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CrimeReportAdapter : ListAdapter<CrimeReport, CrimeReportAdapter.CrimeReportViewHolder>(CrimeReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crime_report, parent, false)
        return CrimeReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: CrimeReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CrimeReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCrimeType: TextView = itemView.findViewById(R.id.tvCrimeType)
        private val tvCrimeAddress: TextView = itemView.findViewById(R.id.tvCrimeAddress)
        private val tvCrimeDate: TextView = itemView.findViewById(R.id.tvCrimeDate)
        private val tvCrimeTime: TextView = itemView.findViewById(R.id.tvCrimeTime)

        fun bind(crime: CrimeReport) {
            tvCrimeType.text = crime.type
            tvCrimeAddress.text = crime.address
            tvCrimeDate.text = crime.date
            tvCrimeTime.text = crime.time
        }
    }

    class CrimeReportDiffCallback : DiffUtil.ItemCallback<CrimeReport>() {
        override fun areItemsTheSame(oldItem: CrimeReport, newItem: CrimeReport): Boolean {
            return oldItem.address == newItem.address &&
                   oldItem.date == newItem.date &&
                   oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItem: CrimeReport, newItem: CrimeReport): Boolean {
            return oldItem == newItem
        }
    }
}

