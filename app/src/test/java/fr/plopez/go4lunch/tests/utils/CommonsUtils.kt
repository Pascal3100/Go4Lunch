package fr.plopez.go4lunch.tests.utils

import fr.plopez.go4lunch.data.model.restaurant.Close
import fr.plopez.go4lunch.data.model.restaurant.Open
import fr.plopez.go4lunch.data.model.restaurant.Period
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.di.NearbyConstants
import java.time.LocalTime

object CommonsUtils {
    const val LATITUDE = "90.0"
    const val LONGITUDE = "0.0"
    const val ZOOM = "15.0"
    const val RADIUS = "1000"

    const val WORKMATE_NAME = "WORKMATE_NAME"
    const val WORKMATE_EMAIL = "WORKMATE_ADDRESS"
    const val WORKMATE_PHOTO_URL = "WORKMATE_PHOTO_URL"

    const val PLACE_ID = "PLACE_ID"
    const val PLACE_NAME = "PLACE_NAME"
    const val OTHER_PLACE_NAME = "OTHER_PLACE_NAME"
    const val OTHER_PLACE_ID = "OTHER_PLACE_ID"
    const val PLACE_ADDRESS = "PLACE_ADDRESS"
    const val PLACE_PHOTO_URL = "PLACE_PHOTO_URL"
    const val PLACE_RATE = "5.0"
    const val PLACE_PHONE_NUMBER = "+33 0 00 00 00 00"
    const val PLACE_WEBSITE = "www.no-web-site.com"

    const val QUERY_TIME_STAMP = 66666666L
    const val LOCATION = "$LATITUDE,$LONGITUDE"
    const val MAX_DISPLACEMENT_TOL = "0.000449"
    const val PERIODS_SEARCH_FIELD = "opening_hours,international_phone_number,website"
    const val ERROR_CODE = 404

    const val NEARBY_KEY = "NEARBY_KEY"
    const val NEARBY_TYPE = "NEARBY_TYPE"
    const val MAX_WIDTH = "1080"
    const val PLACE_PHOTO_API_URL =
        "https://maps.googleapis.com/maps/api/place/photo?" +
                "maxwidth=$MAX_WIDTH&photoreference=$PLACE_PHOTO_URL&key=$NEARBY_KEY"

    fun getDefaultRestaurantOpeningPeriodList(
        periodList: List<Period> = getDefaultPeriodList()
    ) = periodList.map { period ->

        val day = period.open?.day!!
        val openingHour = period.open?.time!!
        val closingHour = period.close?.time!!
        val periodId = "$day$openingHour$closingHour"

        RestaurantOpeningPeriod(
            periodId,
            openingHour = LocalTime.of(
                openingHour.take(2).toInt(),
                openingHour.takeLast(2).toInt()
            ).toString(),
            closingHour = LocalTime.of(
                closingHour.take(2).toInt(),
                closingHour.takeLast(2).toInt()
            ).toString(),
            dayOfWeek = day
        )
    }


    fun getDefaultPeriodList(
        periodList: List<List<String>> = getDefaultHours()
    ) = (3..7).map { day ->
        periodList.map { time ->
            Period(
                Close(
                    day = day,
                    time = time[1]
                ),
                Open(
                    day = day,
                    time = time[0]
                )
            )
        }
    }.flatten()

    private fun getDefaultHours() =
        listOf(listOf("1100", "1400"), listOf("1800", "2230"))

    fun getDefaultRestaurantEntityList() = listOf(
        RestaurantEntity(
            restaurantId = PLACE_ID,
            name = PLACE_NAME,
            address = PLACE_ADDRESS,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            photoUrl = PLACE_PHOTO_URL,
            rate = PLACE_RATE.toFloat(),
            phoneNumber = PLACE_PHONE_NUMBER,
            website = PLACE_WEBSITE
        )
    )

    fun getTwiceRestaurantEntityList() = listOf(
        RestaurantEntity(
            restaurantId = PLACE_ID,
            name = PLACE_NAME,
            address = PLACE_ADDRESS,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            photoUrl = PLACE_PHOTO_URL,
            rate = PLACE_RATE.toFloat(),
            phoneNumber = PLACE_PHONE_NUMBER,
            website = PLACE_WEBSITE
        ),
        RestaurantEntity(
            restaurantId = OTHER_PLACE_ID,
            name = OTHER_PLACE_NAME,
            address = PLACE_ADDRESS,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            photoUrl = PLACE_PHOTO_URL,
            rate = PLACE_RATE.toFloat(),
            phoneNumber = PLACE_PHONE_NUMBER,
            website = PLACE_WEBSITE
        )
    )
}