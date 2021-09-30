package fr.plopez.go4lunch.data

import androidx.room.*
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference

@Dao
interface RestaurantDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurant(entity: RestaurantEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurantOpeningPeriod(openingPeriod: RestaurantOpeningPeriod)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurantOpeningPeriodCrossReference(
        crossReference: RestaurantOpeningPeriodsCrossReference
    )


    @Query("SELECT EXISTS(SELECT restaurant_id FROM restaurant_entity WHERE restaurant_id = :restaurant_id)")
    suspend fun isRestaurantExist(restaurant_id: String): Boolean

    @Transaction
    @Query("SELECT EXISTS(SELECT period_id FROM restaurant_opening_period WHERE period_id = :period_id)")
    suspend fun isPeriodExist(period_id: String): Boolean

    @Transaction
    @Query(
        "SELECT * " +
                "FROM restaurant_query " +
                "WHERE " +
                "(:latitude-latitude)*(:latitude-latitude) + (:longitude-longitude)*(:longitude-longitude) < :displacementTol*:displacementTol " +
                "LIMIT 1;"
    )
    suspend fun getNearestRestaurants(latitude: Double, longitude: Double, displacementTol: Float): List<RestaurantEntity>
}