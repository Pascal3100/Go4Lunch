package fr.plopez.go4lunch.view.main_activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), OnMapReadyCallback {
    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
    }

    private val googleMapsViewModel: GoogleMapsViewModel by viewModels()

    private var onLoadFragment = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get notified when the map is ready to be used.
        getMapAsync(this)
    }

    @InternalCoroutinesApi
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        // Notify the viewModel that Map is ready to work
        googleMapsViewModel.onMapReady()

        googleMap.isMyLocationEnabled = true

        // Notify the viewModel that zoom value has been changed and provides it
        googleMap.setOnCameraMoveListener {
            googleMapsViewModel.OnZoomChanged(googleMap.cameraPosition.zoom)
        }

        lifecycleScope.launchWhenStarted {

            googleMapsViewModel.googleMapViewSharedFlow.collect {

                // TODO : eventually put a custom marker on the user position

                // Just center the camera on the new position
                // The onLoadFragment is used to not animate camera when map is loaded.
                if (onLoadFragment) {
                    onLoadFragment = false
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            it.zoom
                        )
                    )
                } else {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            it.zoom
                        )
                    )
                }
            }
        }
    }
}