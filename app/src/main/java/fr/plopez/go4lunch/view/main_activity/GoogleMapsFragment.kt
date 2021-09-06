package fr.plopez.go4lunch.view.main_activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.ViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GoogleMapsFragment : Fragment(), OnMapReadyCallback {

    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
    }

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var googleMapsFragmentViewModel: GoogleMapsFragmentViewModel

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        googleMapsFragmentViewModel = ViewModelProvider(
            this,
            ViewModelFactory.INSTANCE
        )[GoogleMapsFragmentViewModel::class.java]

        // Start monitoring user location
        googleMapsFragmentViewModel.monitorUserLocation()

        // Build the fragment only if it is not initialized
        if (!this::mapFragment.isInitialized) {

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            mapFragment = SupportMapFragment.newInstance()
            mapFragment.getMapAsync(this)
        }

        // Replace the frameLayout with map fragment
        childFragmentManager
            .beginTransaction()
            .replace(R.id.google_maps_view_fragment_container, mapFragment)
            .commit()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_google_maps_view, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.isMyLocationEnabled = true

        var marker: Marker? = null

        googleMap.setOnCameraMoveListener {
            googleMapsFragmentViewModel.setCurrentZoom(googleMap.cameraPosition.zoom)
        }

        lifecycleScope.launchWhenStarted {
            googleMapsFragmentViewModel.currentLocationStateFlow.collect {
                if (marker == null) {
                    marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(it)
                                    .title("${it.latitude},${it.longitude}")
                    )
                } else {
                    marker?.position = it
                }

                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        it,
                        googleMapsFragmentViewModel.getCurrentZoom()
                    )
                )

            }
        }

    }
}