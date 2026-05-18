package com.example.medimart2

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "MedimartDB", null, 9) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS medicines(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                price INTEGER,
                quantity INTEGER,
                image TEXT,
                expiry_date TEXT,
                username TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sales(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                medicine_name TEXT,
                quantity INTEGER,
                total_price INTEGER,
                date TEXT,
                is_closed INTEGER DEFAULT 0,
                username TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                username TEXT UNIQUE,
                password TEXT,
                phone TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 9) {
            // Ensure medicines table has all columns
            addColumnIfNotExists(db, "medicines", "image", "TEXT")
            addColumnIfNotExists(db, "medicines", "expiry_date", "TEXT")
            addColumnIfNotExists(db, "medicines", "username", "TEXT")
            
            // Ensure sales table has username
            addColumnIfNotExists(db, "sales", "username", "TEXT")
            
            // Normalize data: Replace NULLs with empty strings or default values
            // Use coalesce to handle any existing NULLs in critical columns
            db.execSQL("UPDATE medicines SET expiry_date = '' WHERE expiry_date IS NULL")
            db.execSQL("UPDATE medicines SET image = '' WHERE image IS NULL")
            db.execSQL("UPDATE medicines SET username = 'admin' WHERE username IS NULL OR username = ''")
            db.execSQL("UPDATE sales SET username = 'admin' WHERE username IS NULL OR username = ''")
            
            // Ensure users table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    username TEXT UNIQUE,
                    password TEXT,
                    phone TEXT
                )
            """)
        }
    }

    private fun addColumnIfNotExists(db: SQLiteDatabase, tableName: String, columnName: String, columnType: String) {
        try {
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            var exists = false
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) {
                        exists = true
                        break
                    }
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
            }
        } catch (e: Exception) {
            // If table doesn't exist or other error, fallback to attempting ALTER
            try {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
            } catch (e2: Exception) {}
        }
    }

    fun insertUser(name: String, user: String, pass: String, phn: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("username", user)
            put("password", pass)
            put("phone", phn)
        }
        val result = db.insert("users", null, values)
        return result != -1L
    }

    fun checkUser(user: String, pass: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", arrayOf(user, pass))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkUsernameExists(user: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", arrayOf(user))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun insertMedicine(name: String, price: Int, quantity: Int, image: String, expiryDate: String, currentUser: String): Boolean {
        return try {
            val db = writableDatabase
            val cursor = db.rawQuery("SELECT quantity FROM medicines WHERE name = ? AND expiry_date = ? AND username = ?", arrayOf(name, expiryDate, currentUser))
            val values = ContentValues()
            val result: Long
            if (cursor.moveToFirst()) {
                val newQty = cursor.getInt(0) + quantity
                values.put("quantity", newQty)
                values.put("price", price)
                if (image.isNotEmpty()) values.put("image", image)
                result = db.update("medicines", values, "name = ? AND expiry_date = ? AND username = ?", arrayOf(name, expiryDate, currentUser)).toLong()
            } else {
                values.put("name", name)
                values.put("price", price)
                values.put("quantity", quantity)
                values.put("image", image)
                values.put("expiry_date", expiryDate)
                values.put("username", currentUser)
                result = db.insert("medicines", null, values)
            }
            cursor.close()
            result != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun getMedicinesForUser(currentUser: String): List<MedicineModel> {
        val list = mutableListOf<MedicineModel>()
        val db = readableDatabase
        // Show only medicines that are currently in stock
        val cursor = db.rawQuery("SELECT * FROM medicines WHERE username = ? AND quantity > 0", arrayOf(currentUser))
        
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex("name")
            val priceIndex = cursor.getColumnIndex("price")
            val qtyIndex = cursor.getColumnIndex("quantity")
            val imgIndex = cursor.getColumnIndex("image")
            val expIndex = cursor.getColumnIndex("expiry_date")
            
            do {
                list.add(MedicineModel(
                    name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else "",
                    price = if (priceIndex != -1) cursor.getInt(priceIndex) else 0,
                    quantity = if (qtyIndex != -1) cursor.getInt(qtyIndex) else 0,
                    image = if (imgIndex != -1) cursor.getString(imgIndex) ?: "" else "",
                    expiryDate = if (expIndex != -1) cursor.getString(expIndex) ?: "N/A" else "N/A"
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getStockOutMedicines(currentUser: String): List<MedicineModel> {
        val list = mutableListOf<MedicineModel>()
        val db = readableDatabase
        // Show only medicines where ALL batches of that name are out of stock
        val query = """
            SELECT * FROM medicines 
            WHERE quantity <= 0 
            AND username = ? 
            AND name NOT IN (SELECT name FROM medicines WHERE quantity > 0 AND username = ?)
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(currentUser, currentUser))
        
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex("name")
            val priceIndex = cursor.getColumnIndex("price")
            val qtyIndex = cursor.getColumnIndex("quantity")
            val imgIndex = cursor.getColumnIndex("image")
            val expIndex = cursor.getColumnIndex("expiry_date")
            
            do {
                list.add(MedicineModel(
                    name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else "",
                    price = if (priceIndex != -1) cursor.getInt(priceIndex) else 0,
                    quantity = if (qtyIndex != -1) cursor.getInt(qtyIndex) else 0,
                    image = if (imgIndex != -1) cursor.getString(imgIndex) ?: "" else "",
                    expiryDate = if (expIndex != -1) cursor.getString(expIndex) ?: "N/A" else "N/A"
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun searchMedicine(name: String, currentUser: String): Cursor? {
        return readableDatabase.rawQuery(
            "SELECT id, name, price, quantity, image, expiry_date, username FROM medicines WHERE name LIKE ? AND username = ? AND quantity > 0",
            arrayOf("%$name%", currentUser)
        )
    }

    fun getAllMedicineNames(currentUser: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT name FROM medicines WHERE username = ?", arrayOf(currentUser))
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getGlobalMedicineNames(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT name FROM medicines", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertSale(name: String, qty: Int, total: Int, date: String, currentUser: String) {
        val values = ContentValues().apply {
            put("medicine_name", name)
            put("quantity", qty)
            put("total_price", total)
            put("date", date)
            put("is_closed", 0)
            put("username", currentUser)
        }
        writableDatabase.insert("sales", null, values)
    }

    fun updateStock(name: String, expiryDate: String, soldQty: Int, currentUser: String) {
        writableDatabase.execSQL("UPDATE medicines SET quantity = quantity - ? WHERE name = ? AND expiry_date = ? AND username = ?", arrayOf(soldQty, name, expiryDate, currentUser))
    }

    fun updateStockById(id: Int, soldQty: Int): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT quantity FROM medicines WHERE id = ?", arrayOf(id.toString()))
        if (cursor.moveToFirst()) {
            val currentQty = cursor.getInt(0)
            cursor.close()
            if (currentQty >= soldQty) {
                val values = ContentValues()
                values.put("quantity", currentQty - soldQty)
                val rows = db.update("medicines", values, "id = ?", arrayOf(id.toString()))
                return rows > 0
            }
        } else {
            cursor.close()
        }
        return false
    }

    fun getMedicineById(id: Int): Cursor? {
        return readableDatabase.rawQuery("SELECT * FROM medicines WHERE id = ?", arrayOf(id.toString()))
    }

    fun getOpenSalesRevenue(currentUser: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT CAST(TOTAL(total_price) AS INTEGER) FROM sales WHERE is_closed = 0 AND username = ?", arrayOf(currentUser))
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun closeCurrentShift(currentUser: String) {
        writableDatabase.execSQL("UPDATE sales SET is_closed = 1 WHERE is_closed = 0 AND username = ?", arrayOf(currentUser))
    }

    fun getTotalHistoricalRevenue(currentUser: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT CAST(TOTAL(total_price) AS INTEGER) FROM sales WHERE username = ?", arrayOf(currentUser))
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun updatePassword(username: String, name: String, phone: String, newPass: String): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE username = ? AND name = ? AND phone = ?",
            arrayOf(username, name, phone)
        )
        val exists = cursor.count > 0
        cursor.close()

        if (exists) {
            val values = ContentValues()
            values.put("password", newPass)
            val rows = db.update("users", values, "username = ?", arrayOf(username))
            return rows > 0
        }
        return false
    }
}


