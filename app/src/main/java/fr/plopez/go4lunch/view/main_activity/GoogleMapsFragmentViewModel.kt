package fr.plopez.go4lunch.view.main_activity

import android.util.Log
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.view.model.PositionViewState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleMapsFragmentViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    var currentLocationMutableStateFlow = MutableStateFlow(PositionViewState(LatLng(0.0, 0.0), 0.0F))
    val currentLocationFlow: Flow<PositionViewState> = currentLocationMutableStateFlow

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    fun monitorUserLocation() {
        viewModelScope.launch {
            locationRepository.fetchUpdates().collect {
                //
                // TODO : Send a request to Retrofit with the new location value to update Restaurants list

                currentLocationMutableStateFlow.value = it
            }
        }
    }

    fun setCurrentZoom(zoom: Float) {
        locationRepository.currentZoomMutableStateFlow.value = zoom
    }
}