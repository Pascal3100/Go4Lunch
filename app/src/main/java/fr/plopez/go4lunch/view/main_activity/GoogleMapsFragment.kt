package fr.plopez.go4lunch.view.main_activity

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import fr.plopez.go4lunch.R
import kotlinx.coroutines.InternalCoroutinesApi

class GoogleMapsFragment : Fragment() {

    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
        const val DEFAULT_ZOOM_VALUE = 15
    }

    private lateinit var mapFragment : SupportMapFragment

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var lastKnownLocation: Location? = null

    // TODO : to be injected
    private lateinit var googleMapsFragmentViewModel : GoogleMapsFragmentViewModel

    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        googleMapsFragmentViewModel = GoogleMapsFragmentViewModel(fusedLocationProviderClient)

        // Build the fragment only if it is not initialized
        if (!this::mapFragment.isInitialized) {

            googleMapsFragmentViewModel.monitorUserLocation()

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            mapFragment = SupportMapFragment.newInstance()
            mapFragment.getMapAsync(OnMapReadyCallback { googleMap ->

                googleMapsFragmentViewModel
                    .getCurrentLocationLiveData()?.observe(
                        viewLifecycleOwner, Observer { curUserPosition ->
                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(curUserPosition)
                            .title("Pascualito is here")
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curUserPosition, DEFAULT_ZOOM_VALUE.toFloat()))
                })
            })
        }


        // Replace the frameLayout with map fragment
        childFragmentManager
            .beginTransaction()
            .replace(R.id.google_maps_view_fragment_container, mapFragment)
            .commit()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_google_maps_view, container, false)
    }
}