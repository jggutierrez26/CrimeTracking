package com.example.crimetracking

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import com.example.crimetracking.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import android.view.View

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBackButton()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Map"
                1 -> "Reports"
                2 -> "Community"
                else -> ""
            }
        }.attach()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish() // This will take user back to HomeActivity
        }
    }
}

class CustomInfoWindowAdapter(private val context: FragmentActivity) : GoogleMap.InfoWindowAdapter {
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
