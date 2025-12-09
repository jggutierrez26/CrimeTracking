package com.example.crimetracking.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.net.URL

object CrimePdfScraper {

    suspend fun fetchCrimes(context: Context): List<Crime> {
        PDFBoxResourceLoader.init(context)

        val pdfUrl = "https://dcil.syr.edu/tasks/DCL.pdf"
        val pdfFile = File(context.cacheDir, "crime_data.pdf")

        URL(pdfUrl).openStream().use { input ->
            pdfFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val document = PDDocument.load(pdfFile)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        return parsePdfText(text)
    }

    private fun parsePdfText(text: String): List<Crime> {
        val reports = mutableListOf<Crime>()

        val lines = text.lines()

        for (line in lines) {
            // Skip header rows or empty rows
            if (line.isBlank()) continue
            if (line.contains("Nature", ignoreCase = true)) continue
            if (line.contains("Syracuse University", ignoreCase = true)) continue

            val cols = line.trim().split(Regex("\\s{2,}"))

            if (cols.size < 4) continue

            val offense = cols[0]                                  // Nature – Classification
            val dateTimeReported = cols[2]                          // "12/07/2025 14:32"
            val location = cols[3]                                  // General Location
            val date: String
            val time: String

            if (dateTimeReported.contains(" ")) {
                val parts = dateTimeReported.split(" ")
                date = parts.getOrNull(0) ?: "Unknown"
                time = parts.getOrNull(1) ?: "Unknown"
            } else {
                date = dateTimeReported
                time = "Unknown"
            }

            reports.add(
                Crime(
                    date = date,
                    time = time,
                    location = location,
                    offense = offense
                )
            )
        }

        return reports
    }

}