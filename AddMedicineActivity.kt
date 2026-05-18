package com.example.medimart2

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var name: AutoCompleteTextView
    private lateinit var price: EditText
    private lateinit var quantity: EditText
    private lateinit var expiryDate: EditText
    private lateinit var image: ImageView
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private var imagePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        db = DatabaseHelper(this)
        session = SessionManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        name = findViewById(R.id.name)
        price = findViewById(R.id.price)
        quantity = findViewById(R.id.quantity)
        expiryDate = findViewById(R.id.expiryDate)
        image = findViewById(R.id.image)
        val saveBtn = findViewById<Button>(R.id.save)
        val addImgBtn = findViewById<Button>(R.id.addImage)

        setupAutoComplete()

        expiryDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                expiryDate.setText(sdf.format(selectedDate.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        addImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, 100)
        }

        saveBtn.setOnClickListener {
            val p = price.text.toString().toIntOrNull()
            val q = quantity.text.toString().toIntOrNull()
            val exp = expiryDate.text.toString()
            val medName = name.text.toString()
            val currentUser = session.getUsername() ?: ""

            if (medName.isNotEmpty() && p != null && q != null && exp.isNotEmpty() && currentUser.isNotEmpty()) {
                val success = db.insertMedicine(medName, p, q, imagePath, exp, currentUser)
                if (success) {
                    scheduleNotification(medName, exp)
                    Toast.makeText(this, "Medicine Saved Successfully", Toast.LENGTH_SHORT).show()
                    
                    setupAutoComplete()
                    
                    name.text.clear()
                    price.text.clear()
                    quantity.text.clear()
                    expiryDate.text.clear()
                    image.setImageResource(0)
                    imagePath = ""
                } else {
                    Toast.makeText(this, "Failed to save medicine", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAutoComplete() {
        // Fetch all names from all users for global suggestions
        val medicineNames = db.getGlobalMedicineNames()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medicineNames)
        name.setAdapter(adapter)
    }

    private fun scheduleNotification(medicineName: String, expiryDateStr: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = sdf.parse(expiryDateStr)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.add(Calendar.MONTH, -3)

                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(this, ExpiryNotificationReceiver::class.java).apply {
                        putExtra("medicineName", medicineName)
                        putExtra("expiryDate", expiryDateStr)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        this, medicineName.hashCode(), intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    image.setImageURI(uri)
                    imagePath = uri.toString()
                } catch (e: Exception) {
                    image.setImageURI(uri)
                    imagePath = uri.toString()
                }
            }
        }
    }
}