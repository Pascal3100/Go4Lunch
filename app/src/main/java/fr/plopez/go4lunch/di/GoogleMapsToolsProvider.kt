package fr.plopez.go4lunch.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class FusedLocationProvider {

    @Provides
    fun provideFusedLocationProviderClient(@ApplicationContext context : Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}