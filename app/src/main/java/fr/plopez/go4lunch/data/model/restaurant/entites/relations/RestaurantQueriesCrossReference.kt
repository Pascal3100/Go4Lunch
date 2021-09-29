package fr.plopez.go4lunch.data.model.restaurant.entites.relations

import androidx.room.ColumnInfo
import androidx.room.Entity
@Entity(tableName = "restaurant_queries_cross_ref",
    primaryKeys = ["query_time_stamp", "restaurant_id"])
data class RestaurantQueriesCrossReference(
    @ColumnInfo(name = "query_time_stamp")
    val queryTimeStamp : Long,

    @ColumnInfo(name = "restaurant_id")
    val restaurantId : String
)
