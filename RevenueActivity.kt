package com.example.medimart2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RevenueActivity : AppCompatActivity() {

    private lateinit var todaySalesText: TextView
    private lateinit var historicalTotalText: TextView
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revenue)

        db = DatabaseHelper(this)
        session = SessionManager(this)
        val currentUser = session.getUsername() ?: ""

        todaySalesText = findViewById(R.id.todaySalesText)
        historicalTotalText = findViewById(R.id.historicalTotalText)
        val btnRefresh = findViewById<Button>(R.id.btnRefreshSales)
        val btnCloseDay = findViewById<Button>(R.id.btnCloseDay)
        val btnHistorical = findViewById<Button>(R.id.btnHistoricalTotal)

        // Initial update
        updateRevenueDisplay(currentUser)

        btnRefresh.setOnClickListener {
            updateRevenueDisplay(currentUser)
            Toast.makeText(this, "Revenue Refreshed", Toast.LENGTH_SHORT).show()
        }

        btnCloseDay.setOnClickListener {
            if (currentUser.isNotEmpty()) {
                db.closeCurrentShift(currentUser)
                updateRevenueDisplay(currentUser)
                Toast.makeText(this, "Day Closed. Current Revenue Reset.", Toast.LENGTH_SHORT).show()
            }
        }

        btnHistorical.setOnClickListener {
            if (currentUser.isNotEmpty()) {
                val total = db.getTotalHistoricalRevenue(currentUser)
                historicalTotalText.text = "Combined Total: $$total"
            }
        }
    }

    private fun updateRevenueDisplay(currentUser: String) {
        if (currentUser.isNotEmpty()) {
            val openRevenue = db.getOpenSalesRevenue(currentUser)
            todaySalesText.text = "Today's Total Revenue: $$openRevenue"
        }
    }
}