package fr.plopez.go4lunch.data

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference

@Database(
    entities = [
        RestaurantsQuery::class,
        RestaurantEntity::class,
        RestaurantOpeningPeriod::class,
        RestaurantOpeningPeriodsCrossReference::class
    ],
    version = 1
)
abstract class RestaurantsCacheDatabase : RoomDatabase() {

    abstract val restaurantDAO: RestaurantDAO
}
