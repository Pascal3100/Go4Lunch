package fr.plopez.go4lunch.data.model.restaurant.entites.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod

data class RestaurantWithOpeningPeriods(
    @Embedded
    val restaurant: RestaurantEntity,
    @Relation(
         parentColumn = "id",
         entityColumn = "restaurant_id",
         associateBy = Junction(RestaurantOpeningPeriodsCrossReference::class)
    )
    val openingHours: List<RestaurantOpeningPeriod>
)
