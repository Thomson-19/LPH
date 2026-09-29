package pl.lph.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LphFirebaseMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(
            LOG_TAG,
            "NOWY FCM TOKEN: $token"
        )
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(
            remoteMessage
        )

        Log.d(
            LOG_TAG,
            "FCM messageId=${remoteMessage.messageId}, data=${remoteMessage.data}"
        )

        LphNotificationManager
            .showRemoteMessage(
                this,
                remoteMessage
            )
    }

    companion object {
        private const val LOG_TAG =
            "LPH-FCM"
    }
}
