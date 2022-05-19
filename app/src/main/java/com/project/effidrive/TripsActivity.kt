package com.project.effidrive

import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.effidrive.databinding.ActivityTripsBinding
import com.project.effidrive.databinding.ActivityViewTripDetailsBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TripsActivity : AppCompatActivity(), TripClickListener {
    private lateinit var binding: ActivityTripsBinding
    private lateinit var mAuth: FirebaseAuth
    private var tripList: ArrayList<TripModel> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val df1 = DecimalFormat("#.#")
        val df2 = DecimalFormat("#.##")
        val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss" )
        simpleDateFormatTime.timeZone = TimeZone.getTimeZone("UTC")
        val gcd = Geocoder(this)

        mAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val journeyRef = db.collection("journeys")
        var documentList = journeyRef
            .whereEqualTo("uid", mAuth.currentUser!!.uid)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                for (document in it) {

                    Log.d("TripsActivity", document.toString())
                    var trip = TripModel()
                    var origin = document.getGeoPoint("origin")
                    var destination = document.getGeoPoint("destination")
                    var distance = document.get("distance")
                    var duration = document.get("duration")
                    var rating = document.get("rating")
                    var date = document.getTimestamp("date")
                    var id = document.id

                    trip?.origin = gcd.getFromLocation(origin!!.latitude, origin!!.longitude, 1)[0].locality.toString()
                    trip?.destination = gcd.getFromLocation(destination!!.latitude, destination!!.longitude, 1)[0].locality.toString()
                    trip.distance = df2.format(distance) + " km"
                    trip.duration = simpleDateFormatTime.format(duration).toString()
                    trip.rating = df1.format(rating).toString()
                    trip.date = simpleDateFormatDate.format(date!!.toDate()).toString()
                    trip.tripId = id
                    tripList.add(trip?: TripModel())
                }




                binding.recyclerView.apply {
                    adapter = TripAdapter(tripList, this@TripsActivity)
                    layoutManager =
                        androidx.recyclerview.widget.LinearLayoutManager(this@TripsActivity)
                }

            }
    }

    override fun onClick(trip: TripModel) {
        val intent = Intent(this, ViewTripDetailsActivity::class.java)
        intent.putExtra("id", trip.tripId)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@TripsActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}