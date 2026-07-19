package com.example.leafguardai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CostActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cost)

        val etAcres = findViewById<EditText>(R.id.etAcres)
        val btnCalculate = findViewById<Button>(R.id.btnCalculateCost)
        val layoutResult = findViewById<LinearLayout>(R.id.layoutResult)
        val txtTotalCost = findViewById<TextView>(R.id.txtTotalCost)

        btnCalculate.setOnClickListener {
            val acresString = etAcres.text.toString()
            if (acresString.isNotEmpty()) {
                val acres = acresString.toDouble()
                // Simple logic: Base cost of ₹3000 per acre
                val cost = acres * 3000

                txtTotalCost.text = "₹ ${cost.toInt()}"
                layoutResult.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter acres", Toast.LENGTH_SHORT).show()
            }
        }
    }
}