package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_CLOSING_ALERTS = "compass_closing_alerts"
    const val CHANNEL_DAILY_BRIEFING = "compass_daily_briefing"
    const val CHANNEL_SYSTEM_ALERTS = "compass_system_alerts"

    const val NOTIFICATION_ID_TEST = 1001
    const val NOTIFICATION_ID_CLOSING = 1002
    const val NOTIFICATION_ID_BRIEFING = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Closing & Cutoff Alerts (High Priority)
            val closingChannel = NotificationChannel(
                CHANNEL_CLOSING_ALERTS,
                "Service Closing & Intake Cutoff Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely warnings before free meal programs, food pantries, and walk-in clinics close."
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: Daily Briefings (Default Priority)
            val briefingChannel = NotificationChannel(
                CHANNEL_DAILY_BRIEFING,
                "Daily Navigator Briefings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning day-plan checklists and active service summaries."
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 3: System & Test Notifications
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM_ALERTS,
                "System & Test Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Verification alerts and sound/vibration tests."
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(closingChannel, briefingChannel, systemChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendTestNotification(context: Context, leadTimeMinutes: Int = 30): Boolean {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CLOSING_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ St. Anthony Dining Room — Closing Soon")
            .setContentText("Hot lunch service closes in $leadTimeMinutes min (1:30 PM). Walk-in queue ending.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("St. Anthony Dining Room closes in $leadTimeMinutes minutes (1:30 PM). Hot lunch line ending soon. Tap to view location, walking route, and intake details.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 100, 250))

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun sendClosingAlert(
        context: Context,
        resourceName: String,
        closesAt: String,
        minutesRemaining: Int,
        isClinic: Boolean = false
    ): Boolean {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val serviceType = if (isClinic) "Clinic Walk-in Intake" else "Free Meal Service"
        val title = "⚠️ $resourceName — Closing Soon"
        val body = "$serviceType closes in $minutesRemaining min ($closesAt). Arrive promptly before doors close."

        val builder = NotificationCompat.Builder(context, CHANNEL_CLOSING_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 100, 250))

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CLOSING + resourceName.hashCode().let { if (it < 0) -it else it } % 1000, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun sendDailyBriefing(
        context: Context,
        taskCount: Int,
        openMealCount: Int
    ): Boolean {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌅 Compass SF — Morning Day Briefing"
        val body = "You have $taskCount tasks scheduled for today, and $openMealCount meal programs opening across San Francisco."

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BRIEFING, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
