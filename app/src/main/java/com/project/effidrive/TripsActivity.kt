package com.project.effidrive

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class TripsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trips)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intent = Intent(this@TripsActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}