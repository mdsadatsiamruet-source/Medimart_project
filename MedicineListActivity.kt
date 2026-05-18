package com.example.medimart2

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class MedicineListActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_list)

        db = DatabaseHelper(this)
        session = SessionManager(this)
        val currentUser = session.getUsername() ?: ""

        val listView = findViewById<ListView>(R.id.medicineListView)
        val medicineList = db.getMedicinesForUser(currentUser)

        val adapter = MedicineAdapter(this, medicineList)
        listView.adapter = adapter
    }
}