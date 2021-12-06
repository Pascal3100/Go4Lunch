package fr.plopez.go4lunch.di

import fr.plopez.go4lunch.BuildConfig

data class BuildConfigProvider (
    val key : String = BuildConfig.places_api_key,
)

