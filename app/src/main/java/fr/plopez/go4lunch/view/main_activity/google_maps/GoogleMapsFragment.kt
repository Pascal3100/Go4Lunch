package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.utils.CustomSnackBar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), OnMapReadyCallback {
    //
    companion object {
        fun newInstance(): GoogleMapsFragment {
            return GoogleMapsFragment()
        }
    }

    // TODO @Nino Builder pattern let's go
    private lateinit var snackbar: CustomSnackBar

    private val googleMapsViewModel: GoogleMapsViewModel by viewModels()

    private var onLoadFragment = true

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

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the position flow
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

                // Collect the restaurant list flow
                googleMapsViewModel.listRestaurantViewStateFlow.collect {

                    // TODO : Put a custom marker on the restaurant position

                    // Add markers for proxy restaurants
                    if (it.isNotEmpty()) {
                        it.forEach { restaurantViewSate ->
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(
                                        LatLng(
                                            restaurantViewSate.latitude,
                                            restaurantViewSate.longitude
                                        )
                                    )
                                    .title(restaurantViewSate.name)
                            )
                        }
                    }
                }

                // Collect the request status flow
                googleMapsViewModel.responseStatusStateFlow.collect {
                    when (it) {
                        RestaurantsRepository.ResponseStatus.NoResponse ->
                            snackbar.showWarningSnackBar(getString(R.string.no_response_message))
                        RestaurantsRepository.ResponseStatus.StatusError.IOException ->
                            snackbar.showWarningSnackBar(getString(R.string.no_internet_message))
                        RestaurantsRepository.ResponseStatus.StatusError.HttpException ->
                            snackbar.showWarningSnackBar(getString(R.string.network_error_message))
                    }
                }
            }
        }
    }
}