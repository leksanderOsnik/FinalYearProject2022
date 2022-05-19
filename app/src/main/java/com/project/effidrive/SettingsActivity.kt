package com.project.effidrive

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.effidrive.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        binding.btnLogout.setOnClickListener{
            auth.signOut()
            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonSignIn.setOnClickListener {
            when{
                TextUtils.isEmpty(binding.userName.text.toString().trim(){it <= ' '}) -> binding.userName.error = "Please enter your name"
                else ->{
                    val name = binding.userName.text.toString().trim{it <= ' '}
                    val darkMode = binding.swtDarkMode.isChecked
                    val miles = binding.swtKilometres.isChecked

                    val user = hashMapOf(
                        "uid" to auth.currentUser?.uid,
                        "name" to name,
                        "isDarkMode" to darkMode,
                        "isMiles" to miles
                    )
                    val docRef = db.collection("user")
                        .add(user)
                        .addOnSuccessListener{
                            Toast.makeText(this, "Information Saved", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                        .addOnFailureListener(){
                            Toast.makeText(this, "Failed to add user", Toast.LENGTH_SHORT).show()
                        }

                }
            }
        }

    }


    override fun onBackPressed() {
        super.onBackPressed()

        val intent = Intent(this@SettingsActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}