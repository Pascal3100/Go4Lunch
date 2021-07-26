package fr.plopez.go4lunch.view.main_activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import fr.plopez.go4lunch.R

class GoogleMapsViewFragment : Fragment() {

    private lateinit var mapFragment : SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("Nino", "onCreate() called with: this = [$this], savedInstanceState = [$savedInstanceState]")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Build the fragment only if it not initialized
        if (!this::mapFragment.isInitialized) {

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            mapFragment = SupportMapFragment.newInstance()
            mapFragment.getMapAsync(OnMapReadyCallback {

                // Add a marker in Pechabou and move the camera
                val pechabou = LatLng(43.5, 1.5)
                it.addMarker(
                    MarkerOptions()
                        .position(pechabou)
                        .title("Marker in Pechabou")
                )
                it.moveCamera(CameraUpdateFactory.newLatLng(pechabou))
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