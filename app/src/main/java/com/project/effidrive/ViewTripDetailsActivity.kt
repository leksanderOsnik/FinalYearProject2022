package com.project.effidrive

import android.graphics.Color
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.effidrive.databinding.ActivityViewTripDetailsBinding
import com.project.effidrive.databinding.CurrentTripActivityBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


private lateinit var mMap: GoogleMap
private lateinit var geocoder: Geocoder
private lateinit var binding: ActivityViewTripDetailsBinding
private lateinit var bundle: Bundle


private var journeyPath: MutableList<GeoPoint> = mutableListOf()

class ViewTripDetailsActivity : AppCompatActivity(), OnMapReadyCallback{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) }





    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val gcd = Geocoder(this, Locale.getDefault())
        val df1 = DecimalFormat("#.#")
        val df2 = DecimalFormat("#.##")
        val sdf1 = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val sdf2 = SimpleDateFormat("HH:mm:ss")

        sdf2.timeZone = TimeZone.getTimeZone("UTC")
        bundle = intent.extras!!;
        var tripId = bundle.getString("id")
        val db = Firebase.firestore
        val docRef = db.collection("journeys").document(tripId!!)
        docRef.get()
            .addOnSuccessListener {
                if (it == null) {
                    Log.d("ViewTripDetailsActivity", "No such document")
                } else {
                    Log.d("MainActivity", it.toString())
                    var origin = it.getGeoPoint("origin")
                    var destination = it.getGeoPoint("destination")
                    var distance = it.get("distance")
                    var duration = it.get("duration")
                    var rating = it.get("rating")
                    var avgSpeed = it.get("avgSpeed")
                    var date = it.getTimestamp("date")
                    journeyPath = it.get("coords") as MutableList<GeoPoint>
                    tripId = it.id

                    var originConverted =
                        gcd.getFromLocation(origin!!.latitude, origin!!.longitude, 1)[0].locality
                    var destinationConverted = gcd.getFromLocation(
                        destination!!.latitude,
                        destination!!.longitude,
                        1
                    )[0].locality

                    binding.tvDistance.text = df2.format(distance) + " km"
                    binding.tvEfficiency.text = df1.format(rating)
                    binding.tvDuration.text = sdf2.format(duration)
                    binding.tvDistance.text = df2.format(distance) + " km"
                    binding.tvAvgSpeed.text = df2.format(avgSpeed) + " km/h"
                    binding.tvOrigin.text = originConverted
                    binding.tvDestination.text = destinationConverted

                    val polylineOptions = PolylineOptions()
                    polylineOptions.color(Color.BLUE)
                    polylineOptions.width(5f)
                    for (point in journeyPath) {
                        polylineOptions.add(LatLng(point.latitude, point.longitude))
                    }
                    mMap.addPolyline(polylineOptions)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(journeyPath[journeyPath.size/3].latitude,
                        journeyPath[journeyPath.size/3].longitude), 12f))
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
                }

                fun UpdateUI() {

                }


            }
    }}