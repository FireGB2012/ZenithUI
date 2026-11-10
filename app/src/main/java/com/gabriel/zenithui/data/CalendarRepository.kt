package com.gabriel.zenithui.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Immutable
data class CalendarEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val colour: Int,
)

/** Reads today's events straight from the system calendar. Nothing is stored or sent anywhere. */
object CalendarRepository {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun today(context: Context): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()

        val zone = ZoneId.systemDefault()
        val day = LocalDate.now(zone)
        val startOfDay = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, startOfDay)
            ContentUris.appendId(it, endOfDay)
            it.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
        )

        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Busy"
                        val begin = cursor.getLong(1)
                        val end = cursor.getLong(2)
                        val allDay = cursor.getInt(3) == 1
                        // Android keeps an all-day event's times as midnights in UTC, not the phone's,
                        // so the phone's day also catches yesterday's or tomorrow's: kept only if it
                        // is today's.
                        if (allDay && !allDayOn(begin, end, day)) continue
                        add(
                            CalendarEvent(
                                title = title,
                                startMillis = begin,
                                endMillis = end,
                                allDay = allDay,
                                colour = cursor.getInt(4),
                            )
                        )
                    }
                }
            }
        }.getOrNull().orEmpty()
    }

    /**
     * Whether an all-day event from [begin] to [end] is on [day]. Android keeps the two as midnights
     * in UTC, so they are read as dates in UTC: from the first up to, not including, the one it ends
     * on (or the first alone, for one that ends where it begins).
     */
    fun allDayOn(begin: Long, end: Long, day: LocalDate): Boolean {
        val first = Instant.ofEpochMilli(begin).atOffset(ZoneOffset.UTC).toLocalDate()
        val last = Instant.ofEpochMilli(end).atOffset(ZoneOffset.UTC).toLocalDate()
        return !day.isBefore(first) && (day.isBefore(last) || day == first)
    }

    /**
     * What the Calendar widget lists of today's [events] at [nowMillis]: those not over yet, one under
     * way among them, with the all-day ones first, as an iPhone lists them, and the rest by when they
     * start, no more than [most]. An all-day event lasts the whole of its day wherever the phone is:
     * its times are midnights in UTC, not the phone's, so they are not read against the clock, and
     * [today] has already kept only today's.
     */
    fun upcoming(events: List<CalendarEvent>, nowMillis: Long, most: Int = 3): List<CalendarEvent> =
        events
            .filter { it.allDay || it.endMillis > nowMillis }
            .sortedWith(compareBy<CalendarEvent>({ !it.allDay }, { it.startMillis }))
            .take(most.coerceAtLeast(0))
}
