package fr.plopez.go4lunch.utils

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.data.model.MapLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationUpdatesUseCase constructor(
    // client will be injected
    private val client: FusedLocationProviderClient
) {

    companion object {
        private const val TAG = "LocationUpdatesUseCase"
        private const val MAX_INTERVAL_SECS = 60L
        private const val UPDATE_INTERVAL_SECS = 60L
        private const val FASTEST_UPDATE_INTERVAL_SECS = 10L
        private const val SMALLEST_DISPLACEMENT_METERS = 20F
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    fun fetchUpdates(): Flow<LatLng> = callbackFlow {

        // Settings for location requests
        val locationRequest = LocationRequest().apply {
//            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = SMALLEST_DISPLACEMENT_METERS
            interval = java.util.concurrent.TimeUnit.SECONDS.toMillis(UPDATE_INTERVAL_SECS)
            fastestInterval = java.util.concurrent.TimeUnit.SECONDS.toMillis(
                FASTEST_UPDATE_INTERVAL_SECS)
            maxWaitTime = java.util.concurrent.TimeUnit.SECONDS.toMillis(MAX_INTERVAL_SECS)
        }

        val callBack = object : LocationCallback() {

            // When a result is available we push a MapLocation Object
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation

                Log.d(TAG, "#### onLocationResult : latitude = ${location.latitude}, longitude = ${location.longitude}")
                offer(LatLng(location.latitude, location.longitude))
            }
        }

        client.requestLocationUpdates(locationRequest, callBack, Looper.getMainLooper())

        awaitClose { client.removeLocationUpdates(callBack) }
    }
}