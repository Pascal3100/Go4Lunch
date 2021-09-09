package fr.plopez.go4lunch.data.repositories

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class LocationRepository @Inject constructor() {

    companion object {
        const val DEFAULT_ZOOM_VALUE = 15
    }

    var curLocationMutableStateFlow = MutableStateFlow(LatLng(0.0,0.0))
    val curLocationStateFlow : StateFlow<LatLng> = curLocationMutableStateFlow
    var currentZoom: Float = DEFAULT_ZOOM_VALUE.toFloat()
}