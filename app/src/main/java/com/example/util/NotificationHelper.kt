package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local push notification manager for instant transaction alerts, Account Brought Forward
 * reconciliations, and category budget threshold (80% warning / 100% exceeded) notifications.
 */
object NotificationHelper {
    private const val CHANNEL_TRANSACTIONS = "mymoney_transactions_channel"
    private const val CHANNEL_BUDGETS = "mymoney_budgets_channel"
    private val notificationIdCounter = AtomicInteger(2000)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val txChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Real-Time Transaction & Ledger Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Instant notifications when expenses, incomes, transfers, or Brought Forward adjustments are recorded"
            }
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGETS,
                "Category Budget Threshold Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts when monthly spending reaches 80% warning or 100% budget limits"
            }
            manager.createNotificationChannel(txChannel)
            manager.createNotificationChannel(budgetChannel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendTransactionAlert(
        context: Context,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        return try {
            val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context)
                .notify(notificationIdCounter.incrementAndGet(), notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    fun sendBudgetThresholdAlert(
        context: Context,
        categoryName: String,
        percentUsed: Int,
        formattedSpent: String,
        formattedLimit: String
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        return try {
            val isExceeded = percentUsed >= 100
            val title = if (isExceeded) {
                "Budget Exceeded: $categoryName ($percentUsed%)"
            } else {
                "Budget Warning: $categoryName ($percentUsed%)"
            }
            val body = if (isExceeded) {
                "You have spent $formattedSpent of your $formattedLimit monthly limit in $categoryName."
            } else {
                "Heads up! You've reached $percentUsed% ($formattedSpent / $formattedLimit) of your $categoryName budget."
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_BUDGETS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context)
                .notify(notificationIdCounter.incrementAndGet(), notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }
}
