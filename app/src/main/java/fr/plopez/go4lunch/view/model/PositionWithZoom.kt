package fr.plopez.go4lunch.view.model

import com.google.android.gms.maps.model.LatLng

data class PositionWithZoom (
    val position : LatLng,
    val zoom : Float)