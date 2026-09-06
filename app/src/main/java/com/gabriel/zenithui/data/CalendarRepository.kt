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
import java.time.LocalDate
import java.time.ZoneId

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
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

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
                        add(
                            CalendarEvent(
                                title = title,
                                startMillis = cursor.getLong(1),
                                endMillis = cursor.getLong(2),
                                allDay = cursor.getInt(3) == 1,
                                colour = cursor.getInt(4),
                            )
                        )
                    }
                }
            }
        }.getOrNull().orEmpty()
    }
}
