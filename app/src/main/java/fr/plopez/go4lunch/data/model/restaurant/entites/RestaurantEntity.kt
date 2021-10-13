package fr.plopez.go4lunch.data.model.restaurant.entites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurant_entity"
)
data class RestaurantEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "restaurant_id")
    // Google place_id
    val restaurantId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "photo_url")
    val photoUrl: String?,

    @ColumnInfo(name = "rate")
    val rate: Float,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "website")
    val website: String
)
