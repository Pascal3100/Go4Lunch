package fr.plopez.go4lunch.utils

import java.time.LocalTime
import java.util.*
import javax.inject.Inject

class DateTimeUtils @Inject constructor() {

    fun getCurrentDay():Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun getCurrentTime(): LocalTime = LocalTime.now()
}