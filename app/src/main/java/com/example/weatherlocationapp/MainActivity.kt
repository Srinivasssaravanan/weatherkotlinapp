package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val API: String = "47a097065d990060948a85c7857a168e" 
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blaa)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val cityInput = findViewById<EditText>(R.id.cityInput)
        val fetchWeatherButton = findViewById<Button>(R.id.fetchWeatherButton)
        val currentLocationButton = findViewById<Button>(R.id.currentLocationButton)

        fetchWeatherButton.setOnClickListener {
            val inputCity = cityInput.text.toString().trim()
            if (inputCity.isNotEmpty()) {
                WeatherTask(inputCity).execute()
            } else {
                cityInput.error = "Please enter a city!"
            }
        }

        currentLocationButton.setOnClickListener {
            if (checkPermission()) {
                getCurrentLocation()
            } else {
                requestPermission()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    WeatherTask(null, lat, lon).execute()
                } else {
                    Toast.makeText(applicationContext, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }


    inner class WeatherTask(private val city: String? = null, private val lat: Double? = null, private val lon: Double? = null) :
        AsyncTask<String, Void, String?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            val url = when {
                city != null -> "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$API"
                lat != null && lon != null -> "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$API"
                else -> return null
            }
            return try {
                URL(url).readText(Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
            try {
                if (result == null) throw Exception("No response from server.")

                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val updatedAtText = "Updated at: " +
                        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(jsonObj.getLong("dt") * 1000))
                val temp = "${main.getString("temp")}°C"
                val tempMin = "Min Temp: ${main.getString("temp_min")}°C"
                val tempMax = "Max Temp: ${main.getString("temp_max")}°C"
                val pressure = "Pressure: ${main.getString("pressure")} hPa"
                val humidity = "Humidity: ${main.getString("humidity")}%"
                val windSpeed = "Wind Speed: ${wind.getString("speed")} km/h"
                val weatherDescription = weather.getString("description").replaceFirstChar { it.uppercase() }
                val sunrise = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sys.getLong("sunrise") * 1000))
                val sunset = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sys.getLong("sunset") * 1000))
                val address = "${jsonObj.getString("name")}, ${sys.getString("country")}"

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = humidity
                findViewById<TextView>(R.id.sunrise).text = "Sunrise: $sunrise"
                findViewById<TextView>(R.id.sunset).text = "Sunset: $sunset"

                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            } catch (e: Exception) {
                findViewById<TextView>(R.id.errorText).apply {
                    visibility = View.VISIBLE
                    text = "Error: Unable to fetch weather data."
                }
            }
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}

