package fr.plopez.go4lunch.data.repositories

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.view.model.PositionWithZoom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val client: FusedLocationProviderClient
) {

    companion object {
        private const val MAX_INTERVAL_SECS = 60L
        private const val UPDATE_INTERVAL_SECS = 60L
        private const val FASTEST_UPDATE_INTERVAL_SECS = 10L
        private const val SMALLEST_DISPLACEMENT_METERS = 20F
    }

    var currentZoom: Float = Resources.getSystem().getString(R.string.default_zoom_value).toFloat()

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    fun fetchUpdates(): Flow<PositionWithZoom> = callbackFlow {

        // Settings for location requests
        val locationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = SMALLEST_DISPLACEMENT_METERS
            interval = TimeUnit.SECONDS.toMillis(UPDATE_INTERVAL_SECS)
            fastestInterval = TimeUnit.SECONDS.toMillis(FASTEST_UPDATE_INTERVAL_SECS)
            maxWaitTime = TimeUnit.SECONDS.toMillis(MAX_INTERVAL_SECS)
        }

        val callBack = object : LocationCallback() {

            // When a result is available we push a MapLocation Object
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation

                // trySend replaces offer
                trySend(
                    PositionWithZoom(
                        LatLng(location.latitude, location.longitude),
                        currentZoom
                    )
                )
            }
        }

        client.requestLocationUpdates(locationRequest, callBack, Looper.getMainLooper())

        awaitClose { client.removeLocationUpdates(callBack) }
    }
}