package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String = "Current Location",
    val isManualAnchor: Boolean = false,
    val accuracyMeters: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class NeighborhoodAnchor(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val subtitle: String
)

object LocationHelper {

    val NEIGHBORHOOD_ANCHORS = listOf(
        NeighborhoodAnchor(
            id = "tenderloin",
            name = "Tenderloin",
            latitude = 37.7840,
            longitude = -122.4140,
            subtitle = "Ellis, Golden Gate & Eddy / Civic Center BART"
        ),
        NeighborhoodAnchor(
            id = "civic_center",
            name = "Civic Center",
            latitude = 37.7795,
            longitude = -122.4175,
            subtitle = "City Hall, Main Library & UN Plaza"
        ),
        NeighborhoodAnchor(
            id = "soma",
            name = "SoMa",
            latitude = 37.7780,
            longitude = -122.4050,
            subtitle = "5th–8th & Mission / Howard / Folsom"
        ),
        NeighborhoodAnchor(
            id = "mission",
            name = "Mission District",
            latitude = 37.7600,
            longitude = -122.4190,
            subtitle = "16th & 24th Mission BART corridors"
        ),
        NeighborhoodAnchor(
            id = "western_addition",
            name = "Western Addition",
            latitude = 37.7830,
            longitude = -122.4320,
            subtitle = "Fillmore & Geary / Japantown"
        ),
        NeighborhoodAnchor(
            id = "bayview",
            name = "Bayview-Hunters Point",
            latitude = 37.7340,
            longitude = -122.3900,
            subtitle = "3rd Street Corridor & Palou Ave"
        ),
        NeighborhoodAnchor(
            id = "downtown",
            name = "Downtown / Market",
            latitude = 37.7880,
            longitude = -122.4075,
            subtitle = "Powell Station, Union Square & Market St"
        ),
        NeighborhoodAnchor(
            id = "castro",
            name = "Castro / Duboce",
            latitude = 37.7620,
            longitude = -122.4350,
            subtitle = "18th & Castro / Market & Church"
        ),
        NeighborhoodAnchor(
            id = "sunset_richmond",
            name = "Sunset / Richmond",
            latitude = 37.7630,
            longitude = -122.4600,
            subtitle = "Inner Sunset, UCSF & Golden Gate Park"
        ),
        NeighborhoodAnchor(
            id = "excelsior",
            name = "Excelsior / Ingleside",
            latitude = 37.7200,
            longitude = -122.4430,
            subtitle = "Mission & Geneva / Balboa Park BART"
        )
    )

    private val ADDRESS_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
        "330 ellis st" to Pair(37.7853, -122.4116),
        "121 golden gate ave" to Pair(37.7822, -122.4140),
        "225 potrero ave" to Pair(37.7686, -122.4072),
        "900 pennsylvania ave" to Pair(37.7547, -122.3934),
        "165 capp st" to Pair(37.7634, -122.4187),
        "2111 jennings st" to Pair(37.7265, -122.3921),
        "525 5th st" to Pair(37.7779, -122.4005),
        "134 golden gate ave" to Pair(37.7820, -122.4145),
        "1031 franklin st" to Pair(37.7844, -122.4239),
        "101 8th st" to Pair(37.7772, -122.4132),
        "100 larkin st" to Pair(37.7793, -122.4160),
        "1187 franklin st" to Pair(37.7865, -122.4238),
        "1 dr carlton b goodlett pl" to Pair(37.7792, -122.4192),
        "230 golden gate ave" to Pair(37.7818, -122.4162),
        "1001 polk st" to Pair(37.7862, -122.4201),
        "356 7th st" to Pair(37.7766, -122.4061),
        "4058 18th st" to Pair(37.7607, -122.4357),
        "1060 howard st" to Pair(37.7788, -122.4082),
        "260 golden gate ave" to Pair(37.7815, -122.4170),
        "290 turk st" to Pair(37.7832, -122.4150),
        "1800 market st" to Pair(37.7712, -122.4243),
        "333 turk st" to Pair(37.7831, -122.4165),
        "730 polk st" to Pair(37.7838, -122.4187),
        "1080 bush st" to Pair(37.7891, -122.4168),
        "470 castro st" to Pair(37.7606, -122.4352),
        "127 collingwood st" to Pair(37.7601, -122.4371),
        "37 grove st" to Pair(37.7788, -122.4169),
        "930 bryant st" to Pair(37.7744, -122.4045),
        "201 8th st" to Pair(37.7765, -122.4116),
        "1292 page st" to Pair(37.7709, -122.4439),
        "160 capp st" to Pair(37.7636, -122.4184),
        "1724 bancroft ave" to Pair(37.7262, -122.3929),
        "1500 page st" to Pair(37.7705, -122.4485),
        "270 6th st" to Pair(37.7786, -122.4042),
        "6th st & minna st" to Pair(37.7801, -122.4080),
        "401 3rd st" to Pair(37.7836, -122.3982),
        "505 howard st" to Pair(37.7884, -122.3986),
        "72 6th st" to Pair(37.7812, -122.4082),
        "680 bryant st" to Pair(37.7773, -122.4019),
        "165 capp st" to Pair(37.7635, -122.4183),
        "525 5th st" to Pair(37.7770, -122.3999),
        // EBT Restaurant Meals Program
        "391 golden gate ave" to Pair(37.7809, -122.4199),
        "1200 market st" to Pair(37.7785, -122.4149),
        "2400 mission st" to Pair(37.7588, -122.4188),
        "4800 3rd st" to Pair(37.7329, -122.3908),
        "100 mcallister st" to Pair(37.7806, -122.4153),
        "468 ellis st" to Pair(37.7848, -122.4137),
        "131 6th st" to Pair(37.7804, -122.4067),
        "1 hallidie plaza" to Pair(37.7843, -122.4079),
        "1690 valencia st" to Pair(37.7476, -122.4208),
        "675 broadway" to Pair(37.7979, -122.4063),
        "1315 noriega st" to Pair(37.7538, -122.4780),
        "127 eddy st" to Pair(37.7844, -122.4111),
        "1045 polk st" to Pair(37.7871, -122.4200),
        "1101 potrero ave" to Pair(37.7558, -122.4066),
        "5130 3rd st" to Pair(37.7296, -122.3923),
        "5131 3rd st" to Pair(37.7295, -122.3924),
        "3771 mission st" to Pair(37.7371, -122.4243),
        "3100 mission st" to Pair(37.7469, -122.4203),
        "868a geary st" to Pair(37.7861, -122.4181)
    )

    private val NEIGHBORHOOD_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
        "tenderloin" to Pair(37.7840, -122.4140),
        "civic center" to Pair(37.7795, -122.4175),
        "soma" to Pair(37.7780, -122.4050),
        "south of market" to Pair(37.7780, -122.4050),
        "mission" to Pair(37.7600, -122.4190),
        "western addition" to Pair(37.7830, -122.4320),
        "bayview" to Pair(37.7340, -122.3900),
        "downtown" to Pair(37.7880, -122.4075),
        "mid-market" to Pair(37.7808, -122.4117),
        "union square" to Pair(37.7880, -122.4075),
        "castro" to Pair(37.7620, -122.4350),
        "bernal heights" to Pair(37.7460, -122.4180),
        "sunset" to Pair(37.7550, -122.4700),
        "inner sunset" to Pair(37.7630, -122.4600),
        "chinatown" to Pair(37.7960, -122.4060),
        "potrero hill" to Pair(37.7570, -122.4000),
        "excelsior" to Pair(37.7200, -122.4430),
        "ingleside" to Pair(37.7240, -122.4510)
    )

    /**
     * Resolves the latitude and longitude for a Resource.
     * Uses explicit coordinates if available, then matches address, then neighborhood.
     */
    fun getCoordinates(resource: Resource): Pair<Double, Double>? {
        if (resource.lat != null && resource.lng != null && resource.lat != 0.0) {
            return Pair(resource.lat, resource.lng)
        }
        val cleanAddr = resource.address.lowercase().trim()
        for ((knownAddr, coords) in ADDRESS_COORDINATES) {
            if (cleanAddr.contains(knownAddr) || knownAddr.contains(cleanAddr)) {
                return coords
            }
        }
        val cleanNeigh = resource.neighborhood.lowercase().trim()
        for ((knownNeigh, coords) in NEIGHBORHOOD_COORDINATES) {
            if (cleanNeigh.contains(knownNeigh) || knownNeigh.contains(cleanNeigh)) {
                return coords
            }
        }
        return null
    }

    /**
     * Resolves the latitude and longitude for an RmpLocation.
     */
    fun getCoordinates(rmp: RmpLocation): Pair<Double, Double>? {
        if (rmp.lat != null && rmp.lng != null && rmp.lat != 0.0) {
            return Pair(rmp.lat, rmp.lng)
        }
        val cleanAddr = rmp.address.lowercase().trim()
        for ((knownAddr, coords) in ADDRESS_COORDINATES) {
            if (cleanAddr.contains(knownAddr) || knownAddr.contains(cleanAddr)) {
                return coords
            }
        }
        val cleanNeigh = rmp.neighborhood.lowercase().trim()
        for ((knownNeigh, coords) in NEIGHBORHOOD_COORDINATES) {
            if (cleanNeigh.contains(knownNeigh) || knownNeigh.contains(cleanNeigh)) {
                return coords
            }
        }
        return null
    }

    /**
     * Calculates distance between two points in miles using the Haversine formula.
     */
    fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMiles * c
    }

    /**
     * Formats distance and estimated walking time (e.g. "0.3 mi • ~6 min walk" or "< 250 ft • ~1 min walk").
     */
    fun formatDistanceAndWalk(distMiles: Double): String {
        val walkingMinutes = max(1, (distMiles * 20).roundToInt())
        return when {
            distMiles < 0.05 -> "< 250 ft • ~1 min walk"
            distMiles < 0.1 -> {
                val feet = (distMiles * 5280).roundToInt()
                "${feet} ft • ~1 min walk"
            }
            distMiles < 10.0 -> {
                val formatted = String.format(Locale.US, "%.1f mi", distMiles)
                "$formatted • ~$walkingMinutes min walk"
            }
            else -> {
                val formatted = String.format(Locale.US, "%.0f mi", distMiles)
                "$formatted • ~$walkingMinutes min walk"
            }
        }
    }

    /**
     * Formats a concise distance string (e.g. "0.3 mi" or "250 ft").
     */
    fun formatShortDistance(distMiles: Double): String {
        return when {
            distMiles < 0.05 -> "< 250 ft"
            distMiles < 0.1 -> "${(distMiles * 5280).roundToInt()} ft"
            distMiles < 10.0 -> String.format(Locale.US, "%.1f mi", distMiles)
            else -> String.format(Locale.US, "%.0f mi", distMiles)
        }
    }

    /**
     * Formats concise distance string alias.
     */
    fun formatDistance(distMiles: Double): String = formatShortDistance(distMiles)

    /**
     * Formats estimated walking time.
     */
    fun formatWalkTime(distMiles: Double): String {
        val mins = max(1, (distMiles * 20).roundToInt())
        return "~$mins min walk"
    }

    /**
     * Safely queries the Android LocationManager for the last known location.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        var bestLocation: Location? = null
        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < (bestLocation?.accuracy ?: Float.MAX_VALUE)) {
                    bestLocation = loc
                }
            }
        } catch (e: SecurityException) {
            return null
        } catch (e: Exception) {
            return null
        }
        return bestLocation
    }

    /**
     * Requests a fresh location update with fallback to last known location.
     */
    @SuppressLint("MissingPermission")
    fun requestFreshLocation(
        context: Context,
        onLocationReceived: (Location?) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onLocationReceived(null)
            return
        }

        try {
            val lastKnown = getLastKnownLocation(context)
            if (lastKnown != null && (System.currentTimeMillis() - lastKnown.time) < 120_000) {
                onLocationReceived(lastKnown)
                return
            }

            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                onLocationReceived(lastKnown)
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    onLocationReceived(location)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                listener,
                Looper.getMainLooper()
            )

            // Post safety timeout fallback after 4 seconds
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                try {
                    locationManager.removeUpdates(listener)
                    onLocationReceived(getLastKnownLocation(context))
                } catch (e: Exception) {
                    onLocationReceived(lastKnown)
                }
            }, 4000)

        } catch (e: SecurityException) {
            onLocationReceived(null)
        } catch (e: Exception) {
            onLocationReceived(null)
        }
    }
}
