package fr.plopez.go4lunch.data.model.restaurant.entites.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery

data class QueryWithRestaurants(
    @Embedded
    val query: RestaurantsQuery,
    @Relation(
        parentColumn = "query_time_stamp",
        entityColumn = "restaurant_id",
        associateBy = Junction(RestaurantQueriesCrossReference::class)
    )
    val restaurantList: List<RestaurantEntity>
)
