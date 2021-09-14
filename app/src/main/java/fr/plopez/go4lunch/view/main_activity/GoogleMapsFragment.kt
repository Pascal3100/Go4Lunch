package fr.plopez.go4lunch.view.main_activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), OnMapReadyCallback {
    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
    }

    private val googleMapsFragmentViewModel: GoogleMapsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO @Nino : comme j'injecte le SupportMapFragment j'ai viré la if isInitialized
        // sinon il passe jamais dedans. Right or not?
        // ----- ??? -----
        // Get notified when the map is ready to be used.
        getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        googleMapsFragmentViewModel.onMapReady()

        googleMap.isMyLocationEnabled = true

        googleMap.setOnCameraMoveListener {
            googleMapsFragmentViewModel.onZoomChanged(googleMap.cameraPosition.zoom)
        }

        lifecycleScope.launchWhenStarted {

            googleMapsFragmentViewModel.googleMapViewStateFlow.collect {

                // TODO : eventually put a custom marker on the user position

                // Just center the camera on the new position
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.long), it.zoom)
                )
            }
        }
    }
}