package fr.plopez.go4lunch.view.main_activity

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.repositories.LocationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class GoogleMapsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }


    private val googleMapViewStateMutableStateFlow = MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewSharedFlow = googleMapViewStateMutableStateFlow.asSharedFlow()

    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            onMapReadyMutableStateFlow.flatMapLatest { isMapReady ->
                if (isMapReady) {
                    locationRepository.fetchUpdates().map {
                        //
                        // TODO : Send a request to Retrofit with the new location value to update Restaurants list

                        GoogleMapViewState(
                            it.latitude,
                            it.longitude,
                            it.zoom
                        )
                    }
                } else {
                    // Send a default flow
                    flowOf()
                }
            }.collect {
                googleMapViewStateMutableStateFlow.tryEmit(it)
            }
        }
    }

    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    fun OnZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    // Data Class to emit to the UI
    data class GoogleMapViewState(
        val latitude: Double,
        val longitude: Double,
        val zoom: Float
    )
}