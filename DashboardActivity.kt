package com.example.medimart2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<Button>(R.id.btnAddMedicine).setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        findViewById<Button>(R.id.btnSellMedicine).setOnClickListener {
            startActivity(Intent(this, SellMedicineActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewAll).setOnClickListener {
            startActivity(Intent(this, MedicineListActivity::class.java))
        }

        findViewById<Button>(R.id.btnRevenue).setOnClickListener {
            startActivity(Intent(this, RevenueActivity::class.java))
        }

        findViewById<Button>(R.id.btnStockOut).setOnClickListener {
            startActivity(Intent(this, StockOutActivity::class.java))
        }
    }
}