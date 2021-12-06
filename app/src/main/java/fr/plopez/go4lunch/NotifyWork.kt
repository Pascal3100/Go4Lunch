package fr.plopez.go4lunch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.BitmapFactory
import android.graphics.Color.RED
import android.media.AudioAttributes
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.getDefaultUri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.ListenableWorker.Result.success
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.view.main_activity.list_workmates.WorkmateWithSelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@ExperimentalCoroutinesApi
@HiltWorker
class NotifyWork @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestoreRepository: FirestoreRepository,
) : Worker(context, workerParams) {

    companion object {
        const val NOTIFICATION_ID = "${R.string.app_name}_notification_id"
        const val NOTIFICATION_NAME = "${R.string.app_name}"
        const val NOTIFICATION_CHANNEL = "${R.string.app_name}_channel_01"
        const val NOTIFICATION_WORK = "${R.string.app_name}_notification_work"
        const val RESTAURANT_ID = "RESTAURANT_ID"
        const val USER_EMAIL = "USER_EMAIL"
    }

    override fun doWork(): Result {

        val restaurantId = inputData.getString(RESTAURANT_ID)
        val filteredWorkmatesWithSelectedRestaurantList =
            runBlocking { firestoreRepository.getWorkmatesWithSelectedRestaurants()
                .map { workmatesWithSelectedRestaurantList ->
                    workmatesWithSelectedRestaurantList.filter { it.selectedRestaurantId == restaurantId }
                }.first()
            }

        if (filteredWorkmatesWithSelectedRestaurantList.isNotEmpty()) {
            sendNotification(
                id = inputData.getInt(NOTIFICATION_ID, 0),
                workmatesWithSelectedRestaurantList = filteredWorkmatesWithSelectedRestaurantList,
                currentUserEmail = inputData.getString(USER_EMAIL)!!
            )
        }
        return success()
    }

    private fun sendNotification(
        id: Int,
        workmatesWithSelectedRestaurantList: List<WorkmateWithSelectedRestaurant>,
        currentUserEmail: String
    ) {

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val restaurantName = workmatesWithSelectedRestaurantList.first().selectedRestaurantName
        val workmatesNamesList = if (
            workmatesWithSelectedRestaurantList.size == 1 &&
            workmatesWithSelectedRestaurantList.first().workmateEmail == currentUserEmail) {
            emptyList()
        } else {
            workmatesWithSelectedRestaurantList.mapNotNull { workmateWithSelectedRestaurant ->
                if (workmateWithSelectedRestaurant.workmateEmail != currentUserEmail) {
                    workmateWithSelectedRestaurant.workmateName
                } else {
                    null
                }
            }
        }

        // Build Notification
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_baseline_restaurant_menu_24)
            .setContentTitle(context.resources.getString(R.string.notification_title))
            .setLargeIcon(getBitmap())
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (workmatesNamesList.isNotEmpty()) {
            notification.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.resources.getQuantityString(
                        R.plurals.notification_text_not_alone,
                        workmatesNamesList.size,
                        workmatesNamesList.size.toString(),
                        restaurantName,
                        workmatesNamesList.joinToString(separator = ", ")
                    )
                )
            )
        } else {
            notification.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.resources.getString(
                        R.string.notification_text_alone,
                        restaurantName
                    )
                )
            )
        }

        // Build channel
        if (SDK_INT >= O) {
            notification.setChannelId(NOTIFICATION_CHANNEL)

            val ringtoneManager = getDefaultUri(TYPE_NOTIFICATION)
            val audioAttributes =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    NOTIFICATION_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.enableLights(true)
            channel.lightColor = RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            channel.setSound(ringtoneManager, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(id, notification.build())
    }

    private fun getBitmap() =
        BitmapFactory.decodeResource(context.resources, R.drawable.app_logo_icon)
}