package com.project.effidrive

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.effidrive.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val colRef = db.collection("user")
        colRef
            .whereEqualTo("uid", auth.currentUser?.uid)
            .get()
                .addOnSuccessListener {
                if (it.isEmpty) {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                    else{
                        binding.tvUserName.text = "Hello " + it.documents[0].get("name").toString() +"!"

                    }

                }

        binding.btnStart.setOnClickListener{
            val intent = Intent(this@MainActivity, CurrentTripActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnTrips.setOnClickListener{
            val intent = Intent(this@MainActivity, TripsActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnSettings.setOnClickListener{
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


    }

    override fun onBackPressed() {
        super.onBackPressed()


    }
}