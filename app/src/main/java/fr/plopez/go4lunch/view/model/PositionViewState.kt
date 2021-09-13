package fr.plopez.go4lunch.view.model

import com.google.android.gms.maps.model.LatLng

data class PositionViewState (
    val position : LatLng,
    val zoom : Float)