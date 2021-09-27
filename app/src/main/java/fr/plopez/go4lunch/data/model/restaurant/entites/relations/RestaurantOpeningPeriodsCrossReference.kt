package fr.plopez.go4lunch.data.model.restaurant.entites.relations

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "restaurant_opening_period_cross_ref",
        primaryKeys = ["restaurant_id", "period_id"])
data class RestaurantOpeningPeriodsCrossReference(
    @ColumnInfo(name = "restaurant_id")
    val restaurantId : String,

    @ColumnInfo(name = "period_id")
    val periodId : String
)
