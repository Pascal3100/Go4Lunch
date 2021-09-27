package fr.plopez.go4lunch.data

import androidx.room.*
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference

@Dao
interface RestaurantDAO {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurant(entity: RestaurantEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuery(query: RestaurantsQuery)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurantOpeningPeriod(openingPeriod: RestaurantOpeningPeriod)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurantOpeningPeriodCrossReference(
        crossReference: RestaurantOpeningPeriodsCrossReference
    )

//    @Transaction
//    @Query(
//        "SELECT * " +
//                "FROM restaurant_entity " +
//                "WHERE query_time_stamp = (" +
//                    "SELECT query_time_stamp " +
//                    "FROM restaurant_query " +
//                    "WHERE (" +
//                    ":latitude-latitude)*(:latitude-latitude) + (:longitude-longitude)*(:longitude-longitude) < :radius*:radius " +
//                    "LIMIT 1" +
//                ");")
//    suspend fun getNearestQuery(latitude: Double, longitude: Double, radius: Float): List<RestaurantEntity>

//    @Transaction
//    @Query("SELECT * FROM restaurant_opening_period WHERE restaurant_id = :restaurantId")
//    suspend fun getRestaurantOpeningPeriods(restaurantId: String): List<RestaurantOpeningPeriod>

}