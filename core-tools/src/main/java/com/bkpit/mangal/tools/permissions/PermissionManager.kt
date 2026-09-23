package com.bkpit.mangal.tools.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Central place to check every runtime permission the tool layer might need.
 * Lives in core-tools (not app) so ToolRegistry and every Tool implementation
 * can depend on it without app -> core-tools -> app becoming a cycle.
 *
 * The actual OS permission-request flow (ActivityResultContracts) still lives
 * in the app module's Compose screens — this class only answers "do we have
 * it right now", so ToolRegistry can decide whether to execute a tool or
 * return NeedsPermission for the LLM to turn into a natural-language ask.
 */
class PermissionManager(private val context: Context) {

    enum class MangalPermission(val manifestPermission: String) {
        RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
        SEND_SMS(Manifest.permission.SEND_SMS),
        CALL_PHONE(Manifest.permission.CALL_PHONE),
        READ_CALENDAR(Manifest.permission.READ_CALENDAR),
        WRITE_CALENDAR(Manifest.permission.WRITE_CALENDAR),
        POST_NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS),
    }

    fun isGranted(permission: MangalPermission): Boolean =
        ContextCompat.checkSelfPermission(context, permission.manifestPermission) ==
            PackageManager.PERMISSION_GRANTED

    fun missingFor(required: List<MangalPermission>): List<MangalPermission> =
        required.filterNot { isGranted(it) }
}
