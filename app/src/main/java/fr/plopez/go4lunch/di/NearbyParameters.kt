package fr.plopez.go4lunch.di

import fr.plopez.go4lunch.BuildConfig

data class NearbyParameters (
    val key : String = BuildConfig.places_api_key,
    val type : String = "restaurant"
)

