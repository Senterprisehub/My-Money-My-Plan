package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.util.AutoTrackedAlertManager

/**
 * Live Android NotificationListenerService that listens strictly for real, original incoming
 * Banking app, UPI app (Super Money, Google Pay, PhonePe, Paytm, BHIM, CRED, Amazon Pay, etc.),
 * and Bank SMS notifications in real time and scans active status bar notifications on connect.
 */
class FinancialNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Inspect any real active notifications currently in the status bar upon connection
        try {
            val currentNotifications = activeNotifications ?: return
            for (sbn in currentNotifications) {
                inspectStatusBarNotification(sbn)
            }
        } catch (_: Throwable) {
            // Safely ignore if OS restricts activeNotifications read during early boot
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) {
            inspectStatusBarNotification(sbn)
        }
    }

    private fun inspectStatusBarNotification(sbn: StatusBarNotification) {
        // Never parse notifications posted by My Money My Plan (MMMP) itself
        val pkg = sbn.packageName ?: return
        if (pkg == applicationContext.packageName) return

        // Ignore ongoing media / progress / foreground service notifications
        if (sbn.isOngoing) return

        val notification = sbn.notification ?: return

        // Ignore Android group summaries and promotional/recommendation notification categories
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return
        if (notification.category == Notification.CATEGORY_PROMO ||
            notification.category == Notification.CATEGORY_RECOMMENDATION ||
            notification.category == Notification.CATEGORY_SOCIAL
        ) {
            return
        }

        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        val bodyContent = if (bigText.isNotBlank()) bigText else text
        if (title.isBlank() && bodyContent.isBlank()) return

        AutoTrackedAlertManager.handleNotificationPosted(
            context = applicationContext,
            packageName = pkg,
            title = title,
            body = bodyContent,
            timestamp = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }
}
