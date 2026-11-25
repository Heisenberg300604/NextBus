package com.ncs.nextbus.core.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ncs.nextbus.R
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.Route
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.roundToInt

/** "350 m" / "2.4 km". */
@Composable
fun formatDistance(meters: Double): String =
    if (meters < 1_000) {
        // Round to 10 m, but never show "0 m" for a real (non-zero) distance.
        val rounded = (meters / 10).roundToInt() * 10
        stringResource(R.string.distance_meters, if (meters > 0 && rounded == 0) 10 else rounded)
    } else {
        stringResource(R.string.distance_kilometers, meters / 1_000)
    }

/** "Now" / "4 min" / "1 h 5 min". */
@Composable
fun formatEta(etaSeconds: Int): String {
    if (etaSeconds < 60) return stringResource(R.string.eta_now)
    val minutes = (etaSeconds + 30) / 60
    return if (minutes < 60) {
        pluralStringResource(R.plurals.eta_minutes, minutes, minutes)
    } else {
        stringResource(R.string.eta_hours_minutes, minutes / 60, minutes % 60)
    }
}

/** Duration of a ride: "12 min" / "1 h 5 min". */
@Composable
fun formatDurationMinutes(minutes: Int): String =
    if (minutes < 60) {
        pluralStringResource(R.plurals.eta_minutes, minutes, minutes)
    } else {
        stringResource(R.string.eta_hours_minutes, minutes / 60, minutes % 60)
    }

/** "just now" / "3 min ago" / "2 h ago". */
@Composable
fun formatRelativeAge(ageMillis: Long): String {
    val minutes = (ageMillis / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.time_minutes_ago, minutes, minutes)
        minutes < 24 * 60 -> pluralStringResource(R.plurals.time_hours_ago, minutes / 60, minutes / 60)
        else -> pluralStringResource(R.plurals.time_days_ago, minutes / (24 * 60), minutes / (24 * 60))
    }
}

/** Clock time in the user's 12/24-hour preference. */
fun formatClockTime(context: Context, millis: Long): String = DateFormat.getTimeFormat(context).format(Date(millis))

fun formatDate(context: Context, millis: Long): String = DateFormat.getMediumDateFormat(context).format(Date(millis))

@Composable
fun clockTime(millis: Long): String = formatClockTime(LocalContext.current, millis)

@Composable
fun fare(rupees: Int): String = stringResource(R.string.fare_rupees, rupees)

/** "towards PEC Campus". */
@Composable
fun towards(route: Route, direction: Direction): String =
    stringResource(R.string.towards_stop, route.headsign(direction).name)

/** Current time that re-reads the clock every [intervalMillis]; drives freshness and countdown labels. */
@Composable
fun rememberNow(intervalMillis: Long = 1_000L): State<Long> = produceState(System.currentTimeMillis(), intervalMillis) {
    while (true) {
        value = System.currentTimeMillis()
        delay(intervalMillis)
    }
}
