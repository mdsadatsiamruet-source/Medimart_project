package com.example.medimart2

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StockOutActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_out)

        db = DatabaseHelper(this)
        session = SessionManager(this)
        val currentUser = session.getUsername() ?: ""

        listView = findViewById(R.id.stockOutList)

        if (currentUser.isNotEmpty()) {
            val stockOutList = db.getStockOutMedicines(currentUser)
            if (stockOutList.isEmpty()) {
                Toast.makeText(this, "No medicines are out of stock", Toast.LENGTH_SHORT).show()
            }
            val adapter = MedicineAdapter(this, stockOutList)
            listView.adapter = adapter
        }
    }
}