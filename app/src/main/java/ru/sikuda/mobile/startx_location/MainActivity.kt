package ru.sikuda.mobile.startx_location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ru.sikuda.mobile.startx_location.ui.theme.Startx_locationTheme
import java.util.Date

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    //Clear location
    private lateinit var locationManagerGPS: LocationManager
    private lateinit var locationManagerNet: LocationManager

    var isCheckedGPS = false
    var isCheckedNet = false
    private var tvLocationGPS: String = "-"
    private var tvLocationNet: String = "-"
    private var tvLocationGoogle: String = "-"

    //And Google services
    private var fLocationAvailable: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    //permission for location
    private val PERMISSION_REQUEST1 = 1001
    private val PERMISSION_REQUEST2 = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManagerGPS = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManagerNet = getSystemService(LOCATION_SERVICE) as LocationManager

        fLocationAvailable = isLocationEnabled(this)
        if (fLocationAvailable) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(loc: LocationResult) {
                    for (location in loc.locations) {
                        showLocation(location)
                    }
                }
            }
        }

        setContent {

            Startx_locationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    var tvLocationGPS: String by rememberSaveable { mutableStateOf(value = "-")}
//                    var tvLocationNet: String by rememberSaveable { mutableStateOf(value = "-")}
//                    var tvLocationGoogle: String by rememberSaveable { mutableStateOf(value = "-")}

                    MainScreen( tvLocationGPS, tvLocationNet, tvLocationGoogle)
                }
            }
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            val lm = context.getSystemService(LOCATION_SERVICE) as LocationManager
            lm.isLocationEnabled
        } else {
            // This was deprecated in API 28
            val mode: Int = Settings.Secure.getInt(
                context.contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED )
            if (isCheckedGPS)
                locationManagerGPS.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10f, locationListenerGPS )
            else ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST1
            )

        if( ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
            if(isCheckedNet)
                locationManagerNet.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10f, locationListenerNet)
            else ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST2
            )

        //Google services
        startfusedUpdates()
        checkEnabled()
    }

    private fun startfusedUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //val fusedRequest  = LocationRequest.create()
            val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()


            fusedLocationClient.requestLocationUpdates( fusedRequest,
                locationCallback,
                Looper.getMainLooper())
        }

    }

    override fun onPause() {
        locationManagerGPS.removeUpdates(locationListenerGPS)
        locationManagerNet.removeUpdates(locationListenerNet)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }

    //GPS location
    private val locationListenerGPS: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            checkEnabled()
        }

        override fun onProviderEnabled(provider: String) {
            checkEnabled()
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED )  {
                showLocation(locationManagerGPS.getLastKnownLocation(provider))
            }

        }
    }

    //Net location
    private val locationListenerNet: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            checkEnabled()
        }

        override fun onProviderEnabled(provider: String) {
            checkEnabled()
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ) {
                showLocation(locationManagerNet.getLastKnownLocation(provider))
            }
        }
    }

    private fun showLocation(location: Location?) {
        when (location?.provider) {
            LocationManager.GPS_PROVIDER -> tvLocationGPS = formatLocation( location )
            LocationManager.NETWORK_PROVIDER -> tvLocationNet = formatLocation(location)
            else -> tvLocationGoogle = formatLocation(location)
        }
    }

    private fun formatLocation(location: Location?): String {
        return if (location == null) "-"
        else String.format(
            "Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3\$tF %3\$tT",
            location.latitude, location.longitude, Date(location.time)
        )
    }

    private fun checkEnabled() {
        isCheckedGPS = locationManagerGPS.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isCheckedNet = locationManagerNet.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}

@Composable
fun MainScreen(
    tvLocationGPS: String = "-",
    tvLocationNet: String = "-",
    tvLocationGoogle: String = "-",
) {

//    var tvLocationGPS: String by rememberSaveable { mutableStateOf(value = "-")}
//    var tvLocationNet1: String by rememberSaveable { mutableStateOf(value = "-")}
//    var tvLocationGoogle1: String by rememberSaveable { mutableStateOf(value = "-")}


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column() {

            Row(modifier = Modifier) {
                Text(stringResource(R.string.name_gps))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tvLocationGPS ,
                    textAlign = TextAlign.Center
                )
            }
            Divider()
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier) {
                Text(stringResource(R.string.name_network))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tvLocationNet,
                    textAlign = TextAlign.Center
                )
            }
            Divider()
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier) {
                Text(stringResource(R.string.name_goggle_services))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tvLocationGoogle,
                    textAlign = TextAlign.Center
                )
            }
            Divider()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Startx_locationTheme {
        MainScreen("","","")
    }
}