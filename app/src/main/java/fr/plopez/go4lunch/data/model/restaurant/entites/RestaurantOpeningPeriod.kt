package fr.plopez.go4lunch.data.model.restaurant.entites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "restaurant_opening_period",)
data class RestaurantOpeningPeriod (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "period_id")
    // Concatenation of day id + opening hour + closing hour --> 108001200
    val periodId : String,

    @ColumnInfo(name = "opening_hour")
    val openingHour : String,

    @ColumnInfo(name = "closing_hour")
    val closingHour : String,

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
)