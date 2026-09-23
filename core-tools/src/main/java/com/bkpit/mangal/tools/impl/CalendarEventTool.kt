package com.bkpit.mangal.tools.impl

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.bkpit.mangal.tools.ParamSpec
import com.bkpit.mangal.tools.Tool
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tools.permissions.PermissionManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalendarEventTool(private val context: Context) : Tool {
    override val name = "create_calendar_event"
    override val description =
        "Creates a calendar event with a title, start time (ISO-8601), and duration in minutes."

    override val parameterSchema = mapOf(
        "title" to ParamSpec("string", true, "Event title"),
        "start_time_iso" to ParamSpec("string", true, "Start time, e.g. 2026-09-24T15:00:00"),
        "duration_minutes" to ParamSpec("number", false, "Length of the event, default 60"),
    )

    override val requiredPermissions = listOf(
        PermissionManager.MangalPermission.WRITE_CALENDAR,
        PermissionManager.MangalPermission.READ_CALENDAR,
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val title = args["title"] as? String
            ?: return ToolResult.Failure("title must be a string")
        val startIso = args["start_time_iso"] as? String
            ?: return ToolResult.Failure("start_time_iso must be a string")
        val durationMinutes = (args["duration_minutes"] as? Number)?.toLong() ?: 60L

        val start = runCatching {
            ZonedDateTime.parse(startIso, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()))
        }.getOrElse {
            return ToolResult.Failure("start_time_iso is not a valid ISO-8601 timestamp: $startIso")
        }
        val end = start.plusMinutes(durationMinutes)

        val defaultCalendarId = findWritableCalendarId()
            ?: return ToolResult.Failure("No writable calendar found on this device/account")

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, defaultCalendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start.toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, end.toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return if (uri != null) {
            ToolResult.Success("Created '$title' at $startIso")
        } else {
            ToolResult.Failure("Calendar provider rejected the insert")
        }
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}",
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }
}
