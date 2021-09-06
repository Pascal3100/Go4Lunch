package fr.plopez.go4lunch.view.main_activity

import kotlinx.coroutines.flow.collect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.utils.LocationUpdates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GoogleMapsFragmentViewModel(
    private val client: FusedLocationProviderClient,
    private val locationRepository: LocationRepository
): ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    val currentLocationStateFlow: StateFlow<LatLng> = locationRepository.curLocationStateFlow

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    fun monitorUserLocation() {
        val locationUpdates = LocationUpdates(client)
        viewModelScope.launch {
            locationUpdates
                .fetchUpdates()
                .collect {
                    locationRepository.curLocationMutableStateFlow.value = it
                }
        }
    }

    fun setCurrentZoom(zoom:Float){
        locationRepository.currentZoom = zoom
    }

    fun getCurrentZoom() = locationRepository.currentZoom
}