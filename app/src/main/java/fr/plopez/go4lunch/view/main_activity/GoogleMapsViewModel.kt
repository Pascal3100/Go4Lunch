package fr.plopez.go4lunch.view.main_activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.LocationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleMapsViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    private val googleMapViewStateMutableStateFlow = MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewStateFlow = googleMapViewStateMutableStateFlow.asSharedFlow()

    private val isMapReadyMutableStateFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            isMapReadyMutableStateFlow.flatMapLatest { isMapReady ->
                if (isMapReady) {
                    locationRepository.fetchUpdates().map {
                        GoogleMapViewState(
                            it.position.latitude,
                            it.position.longitude,
                            it.zoom
                        )
                    }
                } else {
                    flowOf()
                }
            }.collect {
                // Just update value to trigger the position on the UI
                googleMapViewStateMutableStateFlow.tryEmit(it)
            }
        }
    }

    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    fun onMapReady() {
        isMapReadyMutableStateFlow.value = true
    }

    data class GoogleMapViewState(
        val lat: Double,
        val long: Double,
        val zoom: Float
    )
}