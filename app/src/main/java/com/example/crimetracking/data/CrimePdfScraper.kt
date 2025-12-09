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

        // download PDF
        URL(pdfUrl).openStream().use { input ->
            pdfFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // parse PDF text
        val document = PDDocument.load(pdfFile)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()

        // Convert PDF lines into structured crime entries
        return parsePdfText(text)
    }

    private fun parsePdfText(text: String): List<Crime> {
        val reports = mutableListOf<Crime>()

        // Splitting lines from PDF
        val lines = text.split("\n")

        // Example pattern matching (adjust based on PDF format!)
        val regex = Regex("""(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2})\s+(.*?)\s{2,}(.*)""")

        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val (date, time, location, offense) = match.destructured
                reports.add(Crime(date, time, location, offense))
            }
        }
        return reports
    }
}