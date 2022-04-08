package com.project.effidrive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.project.effidrive.databinding.CurrentTripActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.squareup.okhttp.*
import org.json.JSONObject
import java.io.IOException
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class CurrentTripActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mainHandler: Handler
    private lateinit var mMap: GoogleMap
    private lateinit var binding: CurrentTripActivityBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var okHttpClient: OkHttpClient

    private var inertiaWorstCase: Float = 0.0f
    private var rollingResistanceWorstCase: Float = 0.0f
    private var airResistanceWorstCase: Float = 0.0f
    private var gradeWorstCase: Float = 0.0f

    private var weatherData: JSONObject? = null
    private var isTracking : Boolean = false
    private var distance : Float = 0.0f
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var origin: GeoPoint? = null
    private var destination: GeoPoint? = null
    private var counter: Int = 0
    private var sdf: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.UK)

    private lateinit var currentLocation: Location
    private lateinit var lastLocation: Location
    private lateinit var firstIntervalLocation: Location

    private var shortTermSpeeds: MutableList<Float> = mutableListOf()
    private var speeds: MutableList<Float> = mutableListOf()
    private var coordinates: MutableList<GeoPoint> = mutableListOf()
    private var accelerations: MutableList<Float> = mutableListOf()
    private var ratings: MutableList<Float> = mutableListOf()
    companion object {

        //Location Request Update Interval in Milliseconds
        const val DEFAULT_UPDATE_INTERVAL = 500L
        const val FASTEST_UPDATE_INTERVAL = 500L

        const val PERMISSION_FINE_LOCATION = 99
        const val CAR_WEIGHT_KG = 1095f
        const val GRAVITY = 9.81f
        const val API_KEY = BuildConfig.OWM_API_KEY
        const val API_URL = "https://api.openweathermap.org/data/2.5/weather"
        const val ROLLING_RESISTANCE_COEFFICIENT = 0.0125f
        const val LINEAR_ROLLING_INERTIA_COEFFICIENT = 1.035f

    }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = CurrentTripActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        sdf.timeZone = TimeZone.getTimeZone("GMT")
        mainHandler =  Handler(Looper.getMainLooper())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                lastLocation = location
            }

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = DEFAULT_UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL
        locationRequest.isWaitForAccurateLocation = true
        locationRequest.smallestDisplacement = 0f

        locationCallback = object:LocationCallback(){
            override fun onLocationResult(locationRequest: LocationResult) {
                super.onLocationResult(locationRequest)
                currentLocation = locationRequest.lastLocation

                if(!isTracking) {
                    firstIntervalLocation = currentLocation
                }


                if(isTracking) {
                    counter++
                }

                if(lastLocation!= null){
                var timeElapsedSinceLastUpdate = ((currentLocation.time+1) - lastLocation.time)
                shortTermSpeeds.add(currentLocation.speed)
                var acceleration = ((currentLocation.speed - lastLocation.speed) / timeElapsedSinceLastUpdate)
                accelerations.add(acceleration)
                }

                lastLocation = currentLocation
                updateUI()


                if(counter >= 20 && isTracking) {
                    calculateEfficiency(shortTermSpeeds, firstIntervalLocation, accelerations)
                    counter = 0
                }


                if(currentLocation!=null && !isTracking){
                    binding.btnStart.isVisible = true
                }


            }


        }

        binding.btnStart.setOnClickListener {
            getWeatherUpdate(currentLocation)
                collectData()
        }

        binding.btnEnd.setOnClickListener {
            saveData()
            isTracking = false
            binding.tvDuration.text = "00:00:00"

        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }


    private fun updateUI(){
        mMap.clear()
        var currPos = LatLng(currentLocation.latitude, currentLocation.longitude)
        mMap.addMarker(MarkerOptions().position(currPos))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currPos))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
        val df = DecimalFormat("#.##")
        var avgRating = getRatingAverage()

        if(startTime != 0L && isTracking){
            if(avgRating.isNaN()) {
                binding.tvEfficiency.text = "0.0"
                binding.tvCurrentSpeed.text = "0.0 km/h"
                binding.tvCurrentAcceleration.text = "0.0 km/h/s"
            }
            else{
            binding.tvEfficiency.text = df.format(avgRating)
            }
            binding.tvDistance.text = df.format(distance) + " km"
            binding.tvDuration.text = (sdf.format(currentLocation.time - startTime))
            binding.tvCurrentSpeed.text = df.format(currentLocation.speed * 3.6) + " km/h"
            binding.tvCurrentAcceleration.text = df.format(accelerations.last()*3.6) + " km/h/s"
        }


    }

    private fun startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_FINE_LOCATION)
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
        locationCallback,
        Looper.getMainLooper())
    }

    private fun stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    private fun getRatingAverage(): Float{
        var sum = 0f
        for(rating in ratings){
            sum += rating
        }
        return sum/ratings.size
    }

    private fun getSpeedAverage(speeds:MutableList<Float>): Float{
        var sum = 0f
        for(speed in speeds){
            sum += speed
        }
        return sum/speeds.size
    }

    private fun getDuration(): Long{
        return endTime - startTime
    }

    private fun collectData(){
        isTracking = true
        accelerations.clear()
        startTime = currentLocation.time
        origin = GeoPoint(currentLocation.latitude, currentLocation.longitude)
        coordinates.add(origin!!)
        binding.btnStart.isVisible = false
        binding.btnEnd.isVisible = true
        firstIntervalLocation = currentLocation

    }

    private fun saveData(){
        endTime = currentLocation.time
        destination = GeoPoint(currentLocation.latitude, currentLocation.longitude)
        coordinates.add(destination!!)

        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        var trip = hashMapOf(
            "avgSpeed" to getSpeedAverage(speeds),
            "coords" to coordinates,
            "date" to Timestamp(startTime),
            "origin" to origin,
            "destination" to destination,
            "distance" to distance,
            "duration" to getDuration(),
            "rating" to getRatingAverage(),
            "uid" to FirebaseAuth.getInstance().currentUser?.uid
        )

        db.collection("journeys")
            .add(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "Trip saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Trip not saved", Toast.LENGTH_SHORT).show()
            }

        binding.btnEnd.isVisible = false
        binding.btnStart.isVisible = true
        speeds.clear()
        accelerations.clear()
        coordinates.clear()
        isTracking = false
        shortTermSpeeds.clear()
        ratings.clear()
        distance = 0f
        startTime = 0L
        endTime = 0L
        counter = 0
    }

    private fun calculateEfficiency(currentSpeeds: MutableList<Float>, previousLocation:Location, accelerations: MutableList<Float>){

        //calculate worst case efficiency
        if(inertiaWorstCase == 0f || airResistanceWorstCase == 0f
            || gradeWorstCase == 0f || rollingResistanceWorstCase == 0f){
            val speed = 38.889f
            val acceleration = 3.0f
            val elevationChange = 2.33f

            inertiaWorstCase = calculateInertia(speed, acceleration)
            airResistanceWorstCase = calculateAirResistance(speed)
            gradeWorstCase = calculateGrade(speed, elevationChange)
            rollingResistanceWorstCase = calculateRollingResistance(speed)

        }

        var shortTermDistance = currentLocation.distanceTo(previousLocation)
        var avgSpeed = getSpeedAverage(currentSpeeds)
        var elevationChange = (currentLocation.altitude - firstIntervalLocation.altitude) /
                ((currentLocation.time - firstIntervalLocation.time) /1000)
        var avgAcceleration = getSpeedAverage(accelerations)

        var rollingResistance = calculateRollingResistance(avgSpeed)
        var airResistance = calculateAirResistance(avgSpeed)
        var grade = calculateGrade(avgSpeed, elevationChange.toFloat())
        var inertia = calculateInertia(avgSpeed, avgAcceleration)

        if(grade < 0){
            grade = 0f
        }

        //calculate the total load of the trip section
        var totalLoad = ((18 * (abs(inertia)/ abs(inertiaWorstCase))) +
                (28 * ((grade)/ (gradeWorstCase))) +
                (18 * ((rollingResistance)/ (rollingResistanceWorstCase))) +
                (36 * ((airResistance)/ airResistanceWorstCase)))


        //calculate the efficiency rating of the trip section
        var rating = 100.0 - totalLoad

        //penalty for idling
        if((avgSpeed * 3600 /1000) < 5 ){
            rating -= 3
        }
        //rewarding for optimal speed
        if((avgSpeed * 3600 /1000) > 50 && (avgSpeed * 3600 /1000) < 90){
            rating += 3
        }


        distance += shortTermDistance/1000
        coordinates.add(GeoPoint(currentLocation.latitude, currentLocation.longitude))
        speeds.add(getSpeedAverage(currentSpeeds) * 3600/ 1000)
        ratings.add(rating.toFloat())
        shortTermSpeeds.clear()
        accelerations.clear()
        firstIntervalLocation = currentLocation

    }

    //D = (P/(Rd * Tk)) * (1 - ((0.378 * Pv)/P))
    //D = Air Density
    //P = Total air pressure in Pascals
    //Rd = gas constant for dry air = 287.05 J/kg/degK
    //RH = Relative Humidity
    //Pv = pressure of water vapour = RH * Es
    //Es = saturation vapour pressure = c0 * 10^((c1 *Tc)/(c2 + Tc)) in mb (1 Pascal = 100mb)
    //c0 = 6.1078
    //c1 = 7.5
    //c2 = 237.3
    //Tk = absolute temperature degK
    //Tc = temperature in degC
    private fun calculateAirDensity(jsonObj: JSONObject): Float {
        //var jsonObj = JSONObject(weatherData.toString())
        var temperature = jsonObj.getJSONObject("main").getDouble("temp")
        var pressure = jsonObj.getJSONObject("main").getDouble("pressure")*100
        var humidity = jsonObj.getJSONObject("main").getDouble("humidity")/100
        var tempC = temperature - 273.15
        var Es = 6.1078 * 10.0.pow(((7.5 * tempC) / (237.3 + tempC)))
        var Pv = humidity * Es * 100
        var D = (pressure / (287.05 * temperature)) * (1 - ((0.378 * Pv)/pressure))
        getWeatherUpdate(currentLocation)
        Log.d("Density", D.toString())
        return D.toFloat()
    }

    // Air resistance = 0.5p *Cd * A * v^3
    // p = Air Density
    // Cd = Drag Coefficient
    // A = Drag Surface Area
    // v = Speed m/s
    private fun calculateAirResistance(speed: Float): Float {
        val dragCoefficient = 0.33f
        val area = 2.12748f
        val density = calculateAirDensity(JSONObject(weatherData.toString()))
        return (0.5f * dragCoefficient * area * density * speed.pow(3))
    }

    // Rolling Resistance = Cr * M * g * v
    // Cr = RollingResistanceCoefficient - Assumed to be 0.0125f
    // M = Mass of the vehicle
    // g = Gravity in m/s
    // v = Speed in m/s
    private fun calculateRollingResistance(speed: Float): Float {
        val coefficientOfRollingResistance = ROLLING_RESISTANCE_COEFFICIENT
        val mass = CAR_WEIGHT_KG
        val gravity = GRAVITY
        return coefficientOfRollingResistance * mass * gravity * speed
    }

    //Inertia = M * v * a * Crl
    // M = Mass of the vehicle
    // v = Speed in m/s
    // a = Acceleration in m/s^2
    //Crl = factor to correct for rotational and linear inertia = Assumed to be 1.035f
    private fun calculateInertia(speed: Float, acceleration: Float): Float{
        val mass = CAR_WEIGHT_KG
        val coefficientOfRollingAndLinearInertia = LINEAR_ROLLING_INERTIA_COEFFICIENT
        return mass * speed * acceleration * coefficientOfRollingAndLinearInertia
    }

    // Hill = M * g * v * d/dt(h)
    // M = Mass of the vehicle
    // g = Gravity in m/s
    // v = Speed in m/s
    // d/dt(h) = Elevation change in m/s
    private fun calculateGrade(speed: Float, elevationChange: Float): Float {
        val mass = CAR_WEIGHT_KG
        val gravity = GRAVITY
        return mass * gravity * speed * elevationChange
    }

    private fun getWeatherUpdate(currentLocation: Location){
        var tempURL = API_URL + "?lat=" + currentLocation.latitude + "&lon=" +
                currentLocation.longitude + "&appid=" + API_KEY
        var request = Request.Builder().url(tempURL).build()
        val client = OkHttpClient()
        var json = JSONObject()
        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(request: Request?, e: IOException?) {
                println("Failed to execute request")
            }

            override fun onResponse(response: Response?) {
                val body = response?.body()?.string()

                json = JSONObject(body)
                weatherData = json
            }
        })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        startLocationUpdates()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intent = Intent(this@CurrentTripActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

