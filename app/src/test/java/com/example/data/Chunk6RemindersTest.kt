package com.example.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class Chunk6RemindersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testDefaultReminderPreferences() {
        val prefs = ReminderPreferences.DEFAULT
        assertFalse("Default reminders should be disabled until user turns them on", prefs.enabled)
        assertEquals(ReminderLeadTime.MINUTES_30, prefs.leadTime)
        assertEquals(30, prefs.leadTime.minutes)
        assertTrue(prefs.notifyMealsClosing)
        assertTrue(prefs.notifyClinicsClosing)
        assertTrue(prefs.notifyDailyBriefing)
        assertTrue(prefs.soundAndVibrate)
    }

    @Test
    fun testReminderLeadTimeFromMinutesValid() {
        assertEquals(ReminderLeadTime.MINUTES_15, ReminderLeadTime.fromMinutes(15))
        assertEquals(ReminderLeadTime.MINUTES_30, ReminderLeadTime.fromMinutes(30))
        assertEquals(ReminderLeadTime.MINUTES_45, ReminderLeadTime.fromMinutes(45))
        assertEquals(ReminderLeadTime.MINUTES_60, ReminderLeadTime.fromMinutes(60))
    }

    @Test
    fun testReminderLeadTimeFromMinutesFallback() {
        assertEquals(ReminderLeadTime.MINUTES_30, ReminderLeadTime.fromMinutes(null))
        assertEquals(ReminderLeadTime.MINUTES_30, ReminderLeadTime.fromMinutes(0))
        assertEquals(ReminderLeadTime.MINUTES_30, ReminderLeadTime.fromMinutes(-10))
        assertEquals(ReminderLeadTime.MINUTES_30, ReminderLeadTime.fromMinutes(999))
    }

    @Test
    fun testReminderPreferencesSerialization() {
        val original = ReminderPreferences(
            enabled = true,
            leadTime = ReminderLeadTime.MINUTES_45,
            notifyMealsClosing = true,
            notifyClinicsClosing = false,
            notifyDailyBriefing = true,
            soundAndVibrate = false
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("MINUTES_45") || serialized.contains("45"))

        val deserialized = json.decodeFromString<ReminderPreferences>(serialized)
        assertEquals(original, deserialized)
        assertTrue(deserialized.enabled)
        assertEquals(ReminderLeadTime.MINUTES_45, deserialized.leadTime)
        assertFalse(deserialized.notifyClinicsClosing)
        assertFalse(deserialized.soundAndVibrate)
    }

    @Test
    fun testUpdatingPreferences() {
        var prefs = ReminderPreferences.DEFAULT
        assertFalse(prefs.enabled)

        prefs = prefs.copy(enabled = true)
        assertTrue(prefs.enabled)

        prefs = prefs.copy(leadTime = ReminderLeadTime.MINUTES_15)
        assertEquals(ReminderLeadTime.MINUTES_15, prefs.leadTime)

        prefs = prefs.copy(notifyMealsClosing = false)
        assertFalse(prefs.notifyMealsClosing)

        prefs = prefs.copy(soundAndVibrate = false)
        assertFalse(prefs.soundAndVibrate)

        // Reset to default
        prefs = ReminderPreferences.DEFAULT
        assertFalse(prefs.enabled)
        assertEquals(ReminderLeadTime.MINUTES_30, prefs.leadTime)
        assertTrue(prefs.notifyMealsClosing)
        assertTrue(prefs.soundAndVibrate)
    }

    @Test
    fun testNotificationChannelsAndConstants() {
        assertEquals("compass_closing_alerts", NotificationHelper.CHANNEL_CLOSING_ALERTS)
        assertEquals("compass_daily_briefing", NotificationHelper.CHANNEL_DAILY_BRIEFING)
        assertEquals("compass_system_alerts", NotificationHelper.CHANNEL_SYSTEM_ALERTS)
        assertTrue(NotificationHelper.NOTIFICATION_ID_TEST > 0)
        assertTrue(NotificationHelper.NOTIFICATION_ID_CLOSING > 0)
        assertTrue(NotificationHelper.NOTIFICATION_ID_BRIEFING > 0)
    }
}
