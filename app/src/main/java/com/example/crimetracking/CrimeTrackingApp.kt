package com.example.crimetracking

import android.app.Application
import com.google.firebase.FirebaseApp

class CrimeTrackingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}

