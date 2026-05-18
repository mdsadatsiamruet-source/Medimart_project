package com.example.medimart2

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class MedicineAdapter(private val context: Context, private val medicines: List<MedicineModel>) : BaseAdapter() {

    override fun getCount(): Int = medicines.size

    override fun getItem(position: Int): Any = medicines[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_medicine, parent, false)
        
        val med = medicines[position]
        
        val img = view.findViewById<ImageView>(R.id.medImage)
        val name = view.findViewById<TextView>(R.id.medName)
        val price = view.findViewById<TextView>(R.id.medPrice)
        val stock = view.findViewById<TextView>(R.id.medStock)
        val expiry = view.findViewById<TextView>(R.id.medExpiry)
        
        name.text = med.name
        price.text = "Price: $${med.price}"
        stock.text = "Stock: ${med.quantity}"
        expiry.text = "Exp: ${med.expiryDate}"
        
        if (!med.image.isNullOrEmpty()) {
            try {
                img.setImageURI(Uri.parse(med.image))
            } catch (e: Exception) {
                img.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } else {
            img.setImageResource(android.R.drawable.ic_menu_report_image)
        }
        
        return view
    }
}

data class MedicineModel(
    val name: String,
    val price: Int,
    val quantity: Int,
    val image: String,
    val expiryDate: String
)