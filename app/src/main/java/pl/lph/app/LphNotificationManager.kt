package pl.lph.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

object LphNotificationManager {

    const val EXTRA_URL = "lph_notification_url"

    const val TOPIC_ARTICLES = "lph_articles"
    const val TOPIC_RACES = "lph_races"
    const val TOPIC_UPDATES = "lph_updates"

    const val CHANNEL_ARTICLES = "lph_articles"
    const val CHANNEL_RACES = "lph_races"
    const val CHANNEL_UPDATES = "lph_updates"

    private const val LOG_TAG = "LPH-FCM"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)

        val articles = NotificationChannel(
            CHANNEL_ARTICLES,
            "LPH — Artykuły",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Nowe artykuły opublikowane w Lidze Późnego Hamowania."
        }

        val races = NotificationChannel(
            CHANNEL_RACES,
            "LPH — Wyścigi",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Przypomnienia o rundach, startach i zmianach terminów."
        }

        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            "LPH — Aktualizacje",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Wyniki i inne ważne aktualizacje LPH."
        }

        manager.createNotificationChannels(
            listOf(
                articles,
                races,
                updates
            )
        )
    }

    fun subscribeToDefaultTopics() {
        subscribe(TOPIC_ARTICLES)
        subscribe(TOPIC_RACES)
        subscribe(TOPIC_UPDATES)
    }

    fun logCurrentToken() {
        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(
                        LOG_TAG,
                        "Nie udało się pobrać tokenu FCM.",
                        task.exception
                    )
                    return@addOnCompleteListener
                }

                Log.d(
                    LOG_TAG,
                    "FCM TOKEN: ${task.result}"
                )
            }
    }

    fun showRemoteMessage(
        context: Context,
        remoteMessage: RemoteMessage
    ) {
        createChannels(context)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title =
            notification?.title
                ?: data["title"]
                ?: "Liga Późnego Hamowania"

        val body =
            notification?.body
                ?: data["body"]
                ?: "Nowa aktualizacja LPH."

        val url =
            data["url"]
                ?: notification?.link?.toString()
                ?: ""

        val eventId =
            data["eventId"]
                ?: data["event_id"]
                ?: remoteMessage.messageId
                ?: url
                ?: System.currentTimeMillis().toString()

        val channelId =
            resolveChannel(
                data["category"]
                    ?: data["type"]
                    ?: ""
            )

        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (url.isNotBlank()) {
                putExtra(
                    EXTRA_URL,
                    url
                )
            }
        }

        val requestCode =
            eventId.hashCode()

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            NotificationCompat.Builder(
                context,
                channelId
            )
                .setSmallIcon(
                    R.drawable.ic_notification
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    body
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(body)
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .setPriority(
                    if (channelId == CHANNEL_RACES) {
                        NotificationCompat.PRIORITY_HIGH
                    } else {
                        NotificationCompat.PRIORITY_DEFAULT
                    }
                )

        try {
            NotificationManagerCompat
                .from(context)
                .notify(
                    requestCode,
                    builder.build()
                )
        } catch (securityException: SecurityException) {
            Log.w(
                LOG_TAG,
                "Brak zgody na wyświetlanie powiadomień.",
                securityException
            )
        }
    }

    private fun subscribe(topic: String) {
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        LOG_TAG,
                        "Subskrypcja FCM OK: $topic"
                    )
                } else {
                    Log.w(
                        LOG_TAG,
                        "Subskrypcja FCM NIEUDANA: $topic",
                        task.exception
                    )
                }
            }
    }

    private fun resolveChannel(
        categoryOrType: String
    ): String {
        val normalized =
            categoryOrType
                .trim()
                .lowercase()

        return when {
            normalized.contains("article") ->
                CHANNEL_ARTICLES

            normalized.contains("race") ||
                    normalized.contains("round") ||
                    normalized.contains("grand_prix") ->
                CHANNEL_RACES

            else ->
                CHANNEL_UPDATES
        }
    }
}
