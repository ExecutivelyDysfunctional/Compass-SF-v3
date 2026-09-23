package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

data class AppIconVariant(
    val key: String,
    val componentName: String,
    val displayName: String,
    val description: String
)

object AppIconManager {
    private const val TAG = "AppIconManager"
    const val DEFAULT_KEY = "classic"
    private const val ALIAS_DEFAULT = "com.example.MainActivityDefault"

    val VARIANTS = listOf(
        AppIconVariant("classic", "com.example.MainActivityDefault", "Compass Rose", "Classic SF navigational gold compass"),
        AppIconVariant("beacon", "com.example.MainActivityBeacon", "Beacon Pulse", "Pulsing amber beacon of hope"),
        AppIconVariant("bridge", "com.example.MainActivityBridge", "Golden Gate", "Golden Gate bridge with north star"),
        AppIconVariant("lantern", "com.example.MainActivityLantern", "Lantern Light", "Warm street lantern guiding light"),
        AppIconVariant("wayfinder", "com.example.MainActivityWayfinder", "Wayfinder", "Modern directional wayfinder arrow")
    )

    fun isValidKey(key: String): Boolean {
        return VARIANTS.any { it.key.equals(key, ignoreCase = true) }
    }

    fun applyAppIcon(context: Context, requestedKey: String): Boolean {
        val safeKey = if (isValidKey(requestedKey)) requestedKey.lowercase() else DEFAULT_KEY
        val targetVariant = VARIANTS.find { it.key == safeKey } ?: VARIANTS.first()

        val pm = context.packageManager
        val packageName = context.packageName

        return try {
            val targetComponent = ComponentName(packageName, targetVariant.componentName)

            // 1. Enable the target launcher component first so there is never 0 launcher icons enabled
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other launcher components
            for (variant in VARIANTS) {
                if (variant.componentName != targetVariant.componentName) {
                    val comp = ComponentName(packageName, variant.componentName)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // Disable MainActivity direct launcher entry if present
            try {
                val mainComp = ComponentName(packageName, "com.example.MainActivity")
                if (pm.getComponentEnabledSetting(mainComp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        mainComp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (_: Exception) {}

            Log.i(TAG, "Successfully enabled launcher alias: ${targetVariant.componentName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error switching app icon alias to $requestedKey: ${e.message}", e)
            // Safety fallback to default alias if failure occurred
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(packageName, ALIAS_DEFAULT),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
            false
        }
    }
}
