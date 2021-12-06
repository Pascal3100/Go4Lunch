package fr.plopez.go4lunch.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.plopez.go4lunch.data.RestaurantsCacheDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RoomProvider {
    @Singleton
    @Provides
    // TODO : add a callBack at database creation to clean up old queries with time tolerance
    fun provideRestaurantCacheDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context.applicationContext,
        RestaurantsCacheDatabase::class.java,
        "restaurants_cache_database"
    ).build()

    @Singleton
    @Provides
    fun provideRestaurantCacheDao(
        db: RestaurantsCacheDatabase
    ) = db.restaurantDAO

}

