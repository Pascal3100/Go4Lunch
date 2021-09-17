package fr.plopez.go4lunch.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.plopez.go4lunch.interfaces.RestaurantNearbyService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Singleton
    @Provides
    fun provideNearbyApi(): RestaurantNearbyService {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/place/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RestaurantNearbyService::class.java)
    }

    @Singleton
    @Provides
    fun provideNearbyParameters():NearbyParameters{
        return NearbyParameters()
    }
}
