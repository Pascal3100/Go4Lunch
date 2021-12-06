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
import fr.plopez.go4lunch.view.main_activity.list_restaurants.OnClickRestaurantListener
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.utils.exhaustive
import fr.plopez.go4lunch.view.main_activity.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
@FlowPreview
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

    private lateinit var onClickRestaurantListener: OnClickRestaurantListener

    private val googleMapsViewModel: GoogleMapsViewModel by viewModels()

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
        googleMapsViewModel.googleMapViewStateLiveData.observe(this) {

            // Add markers for proxy restaurants
            mapMarkersOnMap(googleMap, it)
        }

        // Manage to spawn the toasts messages
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                googleMapsViewModel.googleMapViewActionLiveData.observe(this@GoogleMapsFragment) { googleMapViewAction ->
                    when (googleMapViewAction) {
                        is GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage ->
                            CustomSnackBar.with(requireView())
                                .setMessage(getString(googleMapViewAction.messageResId))
                                .setType(CustomSnackBar.Type.WARNING)
                                .build()
                                .show()

                        is GoogleMapsViewModel.GoogleMapViewAction.MoveCamera ->
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    googleMapViewAction.latLng,
                                    googleMapViewAction.zoom,
                                )
                            )
                        is GoogleMapsViewModel.GoogleMapViewAction.AnimateCamera ->
                            googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    googleMapViewAction.latLng,
                                    googleMapViewAction.zoom,
                                )
                            )
                    }.exhaustive
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

                googleMap.addMarker(
                    MarkerOptions()
                        .position(
                            LatLng(
                                restaurantViewSate.latitude,
                                restaurantViewSate.longitude
                            )
                        )
                        .title(restaurantViewSate.name)
                        .icon(BitmapDescriptorFactory.fromResource(restaurantViewSate.iconDrawable))
                ).also { marker -> marker?.tag = restaurantViewSate.id }
            }
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
}