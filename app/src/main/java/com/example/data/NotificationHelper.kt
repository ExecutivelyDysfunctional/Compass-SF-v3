package com.example.data

import android.Manifest
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

object NotificationHelper {

    const val CHANNEL_ID_MEALS = "channel_meals_sf"
    const val CHANNEL_ID_TASKS = "channel_tasks_sf"
    const val CHANNEL_ID_BRIEFING = "channel_briefing_sf"
    const val CHANNEL_ID_WEATHER = "channel_weather_sf"

    const val CHANNEL_MEALS = CHANNEL_ID_MEALS
    const val CHANNEL_TASKS = CHANNEL_ID_TASKS
    const val CHANNEL_BRIEFING = CHANNEL_ID_BRIEFING
    const val CHANNEL_WEATHER = CHANNEL_ID_WEATHER

    private var channelsCreated = false

    fun createNotificationChannels(context: Context) {
        if (channelsCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Meal & Intake Cutoff Alerts
            val mealChannel = NotificationChannel(
                CHANNEL_ID_MEALS,
                "Meal & Intake Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts before San Francisco free meals and intake lotteries close."
                enableVibration(true)
            }

            // 2. Day Checklist & Task Reminders
            val taskChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Day Checklist Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for scheduled appointments, errands, and daily checklist items."
                enableVibration(true)
            }

            // 3. Morning Navigator Briefing
            val briefingChannel = NotificationChannel(
                CHANNEL_ID_BRIEFING,
                "Morning Navigator Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning briefing with open meals, tasks, and street weather."
            }

            // 4. Weather & Cold Shelter Alerts
            val weatherChannel = NotificationChannel(
                CHANNEL_ID_WEATHER,
                "Severe Weather & Shelter Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cold Night shelter activations, rain gear drops, and extreme heat alerts."
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(mealChannel, taskChannel, briefingChannel, weatherChannel)
            )
            channelsCreated = true
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
        title: String,
        message: String,
        bigText: String? = null,
        subText: String? = "Compass SF",
        vibrate: Boolean = true
    ): Boolean {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(subText)
            .setPriority(
                if (channelId == CHANNEL_ID_MEALS || channelId == CHANNEL_ID_WEATHER)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (vibrate) {
            builder.setVibrate(longArrayOf(0, 250, 100, 250))
        }

        if (!bigText.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun sendTestAlert(context: Context): Boolean {
        return sendNotification(
            context = context,
            notificationId = 9999,
            channelId = CHANNEL_ID_MEALS,
            title = "🔔 Test Alert: St. Anthony's Dining Room",
            message = "Free lunch line closes at 1:30 PM (in 30 minutes). Golden Gate Ave.",
            bigText = "St. Anthony's Foundation free sit-down community lunch service operates 10:00 AM - 1:30 PM at 121 Golden Gate Ave in the Tenderloin. Reminder triggered by Compass SF."
        )
    }

    fun sendMorningBriefingAlert(
        context: Context,
        mealsCount: Int,
        tasksCount: Int,
        weatherBrief: String = "SF Street Weather: 56°F Mild Fog · Cold night shelter not active today."
    ): Boolean {
        val summary = "Today: $mealsCount open meal programs, $tasksCount checklist items pending."
        return sendNotification(
            context = context,
            notificationId = 8888,
            channelId = CHANNEL_ID_BRIEFING,
            title = "🧭 Morning Street Navigator Digest",
            message = summary,
            bigText = "$summary\n\n$weatherBrief\n\nTap to plan your stops on Compass SF."
        )
    }
}
