package fr.plopez.go4lunch.data.model.restaurant.entites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_query")
data class RestaurantsQuery (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "query_time_stamp")
    val queryTimeStamp : Long,

    @ColumnInfo(name = "latitude")
    val latitude : Double,

    @ColumnInfo(name = "longitude")
    val longitude : Double,
)