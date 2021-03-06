package fr.plopez.go4lunch.data

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantQueriesCrossReference

@Database(
    entities = [
        RestaurantsQuery::class,
        RestaurantEntity::class,
        RestaurantOpeningPeriod::class,
        RestaurantOpeningPeriodsCrossReference::class,
        RestaurantQueriesCrossReference::class
    ],
    version = 1
)
abstract class RestaurantsCacheDatabase : RoomDatabase() {

    abstract val restaurantDAO: RestaurantDAO
}
