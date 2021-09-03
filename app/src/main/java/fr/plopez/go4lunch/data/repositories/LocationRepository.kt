package fr.plopez.go4lunch.data.repositories

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationRepository {

    var curLocationMutableStateFlow = MutableStateFlow(LatLng(0.0,0.0))
    val curLocationStateFlow : StateFlow<LatLng> = curLocationMutableStateFlow
}