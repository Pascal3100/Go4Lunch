package fr.plopez.go4lunch.data

import androidx.room.*
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.QueryWithRestaurants
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantQueriesCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods

@Dao
interface RestaurantDAO {

    // Insert a new restaurant reference
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurant(entity: RestaurantEntity)

    // Insert a new query
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: RestaurantsQuery)

    // Insert a new opening period
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurantOpeningPeriod(openingPeriod: RestaurantOpeningPeriod)

    // Insert the cross reference between restaurant and opening period because of n m relation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurantOpeningPeriodCrossReference(
        crossReference: RestaurantOpeningPeriodsCrossReference
    )

    // Insert the cross reference between query and restaurants because of n m relation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurantQueriesCrossReference(
        crossReference: RestaurantQueriesCrossReference
    )

    // Get the list of restaurants near the current position if it was covered by a previous request
    @Transaction
    @Query(
        "SELECT * " +
                "FROM restaurant_query " +
                "WHERE " +
                "(:latitude-latitude)*(:latitude-latitude) + (:longitude-longitude)*(:longitude-longitude) < :displacementTol*:displacementTol " +
                "LIMIT 1;"
    )
    suspend fun getNearestRestaurants(latitude: Double, longitude: Double, displacementTol: Float): List<QueryWithRestaurants>

    @Transaction
    @Query(
        "SELECT * " +
                "FROM restaurant_entity " +
                "WHERE restaurant_id = (" +
                    "SELECT restaurant_id " +
                    "FROM restaurant_queries_cross_ref " +
                    "WHERE query_time_stamp = :queryTimeStamp" +
                ");"
    )
    suspend fun getCurrentRestaurants(queryTimeStamp:Long): List<RestaurantWithOpeningPeriods>

    @Query("SELECT * FROM restaurant_query WHERE query_time_stamp = :timestamp LIMIT 1")
    suspend fun getPositionForTimestamp(timestamp: Long): RestaurantsQuery
}