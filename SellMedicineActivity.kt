package com.example.medimart2

import android.database.Cursor
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SellMedicineActivity : AppCompatActivity() {

    private lateinit var searchBox: AutoCompleteTextView
    private lateinit var resultText: TextView
    private lateinit var sellQtyInput: EditText
    private lateinit var searchImage: ImageView
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private var selectedId: Int = -1
    private var selectedName: String? = null
    private var selectedExpiryDate: String? = null
    private var alertRingtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sell_only)

        db = DatabaseHelper(this)
        session = SessionManager(this)
        val currentUser = session.getUsername() ?: ""

        searchBox = findViewById(R.id.searchBox)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        resultText = findViewById(R.id.searchResultText)
        sellQtyInput = findViewById(R.id.sellQuantity)
        searchImage = findViewById(R.id.searchImage)
        val btnSell = findViewById<Button>(R.id.btnSell)

        // Setup AutoComplete with Global Suggestions
        setupAutoComplete()

        btnSearch.setOnClickListener {
            val nameQuery = searchBox.text.toString().trim()
            if (nameQuery.isEmpty()) {
                Toast.makeText(this, "Enter name to search", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cursor = db.searchMedicine(nameQuery, currentUser)
            if (cursor != null && cursor.moveToFirst()) {
                val results = StringBuilder()
                var firstExpiry: String? = null
                var firstImage: String? = null
                var firstId: Int = -1
                val actualName = cursor.getString(1)
                
                selectedName = actualName
                // Update search box without triggering dropdown
                searchBox.setText(actualName, false)
                
                do {
                    val mId = cursor.getInt(0)
                    val mName = cursor.getString(1)
                    val mPrice = cursor.getInt(2)
                    val mQty = cursor.getInt(3)
                    val mImg = cursor.getString(4)
                    val rawExp = cursor.getString(5) ?: ""
                    val displayExp = if (rawExp.isEmpty()) "N/A" else rawExp
                    
                    if (firstId == -1) firstId = mId
                    if (firstExpiry == null) firstExpiry = rawExp
                    if (firstImage == null) firstImage = mImg
                    
                    results.append("Name: $mName | Exp: $displayExp | Stock: $mQty | Price: $mPrice\n\n")
                } while (cursor.moveToNext())
                
                resultText.text = results.toString()
                
                // --- FIX: Intelligent ID Selection ---
                // We previously just took the first ID. 
                // Now we find the first entry that actually has stock.
                cursor.moveToFirst()
                var foundAvailable = false
                do {
                    val mId = cursor.getInt(0)
                    val mQty = cursor.getInt(3)
                    if (mQty > 0) {
                        selectedId = mId
                        selectedExpiryDate = cursor.getString(5) ?: ""
                        foundAvailable = true
                        break
                    }
                } while (cursor.moveToNext())
                
                // If all batches are empty, still select the first one so the user sees "Not enough stock"
                if (!foundAvailable) {
                    cursor.moveToFirst()
                    selectedId = cursor.getInt(0)
                    selectedExpiryDate = cursor.getString(5) ?: ""
                }
                // --------------------------------------

                // Show Image
                if (!firstImage.isNullOrEmpty()) {
                    searchImage.visibility = View.VISIBLE
                    try {
                        searchImage.setImageURI(Uri.parse(firstImage))
                    } catch (e: Exception) {
                        searchImage.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                } else {
                    searchImage.visibility = View.GONE
                }
                
                cursor.close()
            } else {
                resultText.text = "Medicine not found"
                selectedId = -1
                selectedName = null
                selectedExpiryDate = null
                searchImage.visibility = View.GONE
                Toast.makeText(this, "No medicine matches '$nameQuery'", Toast.LENGTH_SHORT).show()
            }
        }

        btnSell.setOnClickListener {
            val sQty = sellQtyInput.text.toString().toIntOrNull()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (selectedId != -1 && sQty != null && currentUser.isNotEmpty()) {
                val cursor = db.readableDatabase.rawQuery(
                    "SELECT name, quantity, price, expiry_date FROM medicines WHERE id = ?", 
                    arrayOf(selectedId.toString())
                )
                
                if (cursor.moveToFirst()) {
                    val nameVal = cursor.getString(0)
                    val stock = cursor.getInt(1)
                    val priceVal = cursor.getInt(2)
                    val expVal = cursor.getString(3) ?: ""
                    
                    if (stock >= sQty) {
                        db.updateStockById(selectedId, sQty)
                        db.insertSale(nameVal, sQty, priceVal * sQty, date, currentUser)
                        Toast.makeText(this, "Sold: $nameVal ($sQty units)", Toast.LENGTH_SHORT).show()
                        
                        if (stock == sQty) {
                            showEmptyStockAlert(nameVal)
                        }
                        
                        resultText.text = "Sold $sQty of $nameVal. Refresh search to see updated stock."
                        sellQtyInput.text.clear()
                    } else {
                        Toast.makeText(this, "Not enough stock! Available: $stock", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: Record not found. Please search again.", Toast.LENGTH_SHORT).show()
                }
                cursor.close()
            } else {
                if (selectedId == -1) {
                    Toast.makeText(this, "Search medicine first", Toast.LENGTH_SHORT).show()
                } else if (sQty == null) {
                    Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEmptyStockAlert(medicineName: String) {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            alertRingtone = RingtoneManager.getRingtone(applicationContext, notification)
            alertRingtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AlertDialog.Builder(this)
            .setTitle("Medicine Empty!")
            .setMessage("The medicine '$medicineName' is now out of stock.")
            .setPositiveButton("OK") { dialog, _ ->
                alertRingtone?.stop()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        alertRingtone?.stop()
        super.onDestroy()
    }

    private fun setupAutoComplete() {
        // Fetch all names from all users for global suggestions
        val medicineNames = db.getGlobalMedicineNames()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medicineNames)
        searchBox.setAdapter(adapter)
    }
}