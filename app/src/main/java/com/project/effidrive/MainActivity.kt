package com.project.effidrive

import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.view.get
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.type.LatLng
import com.project.effidrive.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.ZoneOffset.UTC
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var geocoder: Geocoder
    private lateinit var tripId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Firebase.firestore

        val df1 = DecimalFormat("#.#")
        val df2 = DecimalFormat("#.##")
        val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss" )
        simpleDateFormatTime.timeZone = TimeZone.getTimeZone("UTC")
        val gcd = Geocoder(this)

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

        //get latest journey from database
        val journeyRef = db.collection("journeys")
        journeyRef
            .whereEqualTo("uid", auth.currentUser!!.uid)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    Log.d("MainActivity", "No documents")
                    binding.cardJourneySummary.visibility = android.view.View.GONE
            }else{
                Log.d("MainActivity", it.documents[0].toString())
                    binding.cardJourneySummary.visibility = android.view.View.VISIBLE
                    var origin = it.documents[0].getGeoPoint("origin")
                    var destination = it.documents[0].getGeoPoint("destination")
                    var distance = it.documents[0].get("distance")
                    var duration = it.documents[0].get("duration")
                    var rating = it.documents[0].get("rating")
                    var date = it.documents[0].getTimestamp("date")
                    tripId = it.documents[0].id

                    var originConverted = gcd.getFromLocation(origin!!.latitude, origin!!.longitude, 1)[0].locality
                    var destinationConverted = gcd.getFromLocation(destination!!.latitude, destination!!.longitude, 1)[0].locality

                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvOrigin).text = originConverted
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvDestination).text = destinationConverted
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvDistanceContent).text = df2.format(distance) + " km"
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvDurationContent).text = simpleDateFormatTime.format(duration)
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvRating).text = df1.format(rating)
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvDateContent).text = simpleDateFormatDate.format(date!!.toDate())
                    binding.cardJourneySummary.findViewById<TextView>(R.id.tvTripId).text = tripId


                    Log.d("Origin",""+ originConverted.toString())
                    Log.d("Destination","" +destinationConverted.toString())




                }
            }


        binding.cardJourneySummary.setOnClickListener {
            val intent = Intent(this@MainActivity, ViewTripDetailsActivity::class.java).putExtra("id", tripId)
            startActivity(intent)
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