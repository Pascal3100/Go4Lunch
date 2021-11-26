package fr.plopez.go4lunch.di

import fr.plopez.go4lunch.BuildConfig

data class NearbyConstants (
    val key : String = BuildConfig.places_api_key,
)

