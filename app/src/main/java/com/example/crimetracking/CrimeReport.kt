package com.example.crimetracking

data class CrimeReport(
    val address: String = "",
    val type: String = "",
    val date: String = "",
    val time: String = "",
    val rawDate: String = "",
    val rawTime: String = ""
)

