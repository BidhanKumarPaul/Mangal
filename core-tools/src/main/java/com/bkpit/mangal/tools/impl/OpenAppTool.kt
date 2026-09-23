package com.bkpit.mangal.tools.impl

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.bkpit.mangal.tools.ParamSpec
import com.bkpit.mangal.tools.Tool
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tools.permissions.PermissionManager

class OpenAppTool(private val context: Context) : Tool {
    override val name = "open_app"
    override val description = "Opens an installed app by its display name (fuzzy match)."

    override val parameterSchema = mapOf(
        "app_name" to ParamSpec("string", true, "Display name as the user said it, e.g. 'WhatsApp'"),
    )

    override val requiredPermissions: List<PermissionManager.MangalPermission> = emptyList()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val query = (args["app_name"] as? String)?.trim()?.lowercase()
            ?: return ToolResult.Failure("app_name must be a string")

        val pm = context.packageManager
        val installedApps: List<ApplicationInfo> = pm.getInstalledApplications(0)

        // Exact label match first, then "contains", to avoid e.g. "Maps" -> "Google Maps Go".
        val candidates = installedApps
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it to pm.getApplicationLabel(it).toString() }

        val exact = candidates.firstOrNull { it.second.lowercase() == query }
        val contains = candidates.firstOrNull { it.second.lowercase().contains(query) }
        val match = exact ?: contains
            ?: return ToolResult.Failure("No installed app matches '$query'")

        val launchIntent = pm.getLaunchIntentForPackage(match.first.packageName)
            ?: return ToolResult.Failure("Found '${match.second}' but it has no launcher activity")

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ToolResult.Success("Opening ${match.second}")
    }
}
