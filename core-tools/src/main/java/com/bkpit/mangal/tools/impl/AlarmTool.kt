package com.bkpit.mangal.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.bkpit.mangal.tools.ParamSpec
import com.bkpit.mangal.tools.Tool
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tools.permissions.PermissionManager

/**
 * Uses the public AlarmClock intent API rather than AlarmManager.setExact,
 * because AlarmClock.ACTION_SET_ALARM/ACTION_SET_TIMER requires no dangerous
 * permission and hands the UI off to whatever clock app the user already has
 * — no need to build/maintain our own alarm-firing + ringing UI at all.
 * SCHEDULE_EXACT_ALARM in the manifest is kept for a future custom-reminder
 * path (Phase 5 conversation-triggered reminders) that bypasses the clock app.
 */
class AlarmTool(private val context: Context) : Tool {
    override val name = "set_alarm"
    override val description =
        "Sets a device alarm for a specific time of day, optionally repeating on given days."

    override val parameterSchema = mapOf(
        "hour" to ParamSpec("number", true, "Hour in 24h format, 0-23"),
        "minute" to ParamSpec("number", true, "Minute, 0-59"),
        "label" to ParamSpec("string", false, "Label shown on the alarm"),
    )

    override val requiredPermissions: List<PermissionManager.MangalPermission> = emptyList()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val hour = (args["hour"] as? Number)?.toInt()
            ?: return ToolResult.Failure("hour must be a number")
        val minute = (args["minute"] as? Number)?.toInt()
            ?: return ToolResult.Failure("minute must be a number")
        if (hour !in 0..23 || minute !in 0..59) {
            return ToolResult.Failure("hour/minute out of range")
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            (args["label"] as? String)?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Alarm set for %02d:%02d".format(hour, minute))
        } else {
            ToolResult.Failure("No clock app available to handle the alarm intent")
        }
    }
}
