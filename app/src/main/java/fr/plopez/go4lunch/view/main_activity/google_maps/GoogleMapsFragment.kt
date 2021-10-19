package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.interfaces.OnClickRestaurantListener
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.main_activity.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ClassCastException


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class GoogleMapsFragment :
    SupportMapFragment(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnInfoWindowClickListener {
    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
    }

    // TODO @Nino Builder pattern let's go
    private lateinit var snackbar: CustomSnackBar

    private lateinit var onClickRestaurantListener: OnClickRestaurantListener

    private val googleMapsViewModel: GoogleMapsViewModel by viewModels()

    private var onLoadFragment = true

    private var allMarkers = listOf<Marker>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            onClickRestaurantListener = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement MainActivity"
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get notified when the map is ready to be used.
        getMapAsync(this)

        // TODO @Nino Builder pattern let's go
        snackbar = CustomSnackBar(requireView(), requireContext())
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        // Notify the viewModel that Map is ready to work
        googleMapsViewModel.onMapReady()

        googleMap.isMyLocationEnabled = true

        // Notify the viewModel that zoom value has been changed and provides it
        googleMap.setOnCameraMoveListener {
            googleMapsViewModel.onZoomChanged(googleMap.cameraPosition.zoom)
        }

        // Listener to show restaurant name above marker
        googleMap.setOnMarkerClickListener(this)

        // Listener to show restaurant details on click on marker title
        googleMap.setOnInfoWindowClickListener(this)

        // Manage camera and map the markers on the map
        googleMapsViewModel.googleMapViewStateLiveData.observe(requireActivity()) {

            // Just center the camera on the new position
            // The onLoadFragment is used to not animate camera when map is loaded.
            manageCamera(googleMap, it)

            // Add markers for proxy restaurants
            mapMarkersOnMap(googleMap, it)
        }

        // Manage to spawn the toasts messages
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                googleMapsViewModel.googleMapViewActionFlow.collect {
                    when (it) {
                        is GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage ->
                            snackbar.showWarningSnackBar(getString(it.messageResId))
                    }
                }
            }

        }
    }

    // Add markers for proxy restaurants
    private fun mapMarkersOnMap(
        googleMap: GoogleMap,
        googleMapViewState: GoogleMapsViewModel.GoogleMapViewState
    ) {
        clearAllMarkers()
        if (googleMapViewState.restaurantList.isNotEmpty()) {
            allMarkers = googleMapViewState.restaurantList.mapNotNull { restaurantViewSate ->

                val icon = when (restaurantViewSate.rate) {
                    1.0f -> BitmapDescriptorFactory.fromResource(R.drawable.red_pin_128px)
                    2.0f -> BitmapDescriptorFactory.fromResource(R.drawable.orange_pin_128px)
                    3.0f -> BitmapDescriptorFactory.fromResource(R.drawable.green_pin_128px)
                    else -> BitmapDescriptorFactory.fromResource(R.drawable.grey_pin_128px)
                }

                googleMap.addMarker(
                    MarkerOptions()
                        .position(
                            LatLng(
                                restaurantViewSate.latitude,
                                restaurantViewSate.longitude
                            )
                        )
                        .title(restaurantViewSate.name)
                        .icon(icon)
                ).also { marker -> marker?.tag = restaurantViewSate.id }
            }
        }
    }

    // Just center the camera on the new position
    // The onLoadFragment is used to not animate camera when map is loaded.
    private fun manageCamera(
        googleMap: GoogleMap,
        googleMapViewState: GoogleMapsViewModel.GoogleMapViewState
    ) {
        if (onLoadFragment) {
            onLoadFragment = false
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(googleMapViewState.latitude, googleMapViewState.longitude),
                    googleMapViewState.zoom
                )
            )
        } else {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(googleMapViewState.latitude, googleMapViewState.longitude),
                    googleMapViewState.zoom
                )
            )
        }
    }

    private fun clearAllMarkers() {
        allMarkers.forEach {
            it.remove()
        }
        allMarkers = emptyList()
    }

    // Listener to show restaurant details on click on marker
    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return true
    }

    override fun onInfoWindowClick(marker: Marker) {
        onClickRestaurantListener.onClickRestaurant(marker.tag as String)
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onStart() {
        super.onStart()

    }

}