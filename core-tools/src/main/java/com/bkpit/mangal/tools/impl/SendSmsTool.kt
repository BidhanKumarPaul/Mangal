package com.bkpit.mangal.tools.impl

import android.content.Context
import android.telephony.SmsManager
import com.bkpit.mangal.tools.ParamSpec
import com.bkpit.mangal.tools.Tool
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tools.permissions.PermissionManager

class SendSmsTool(private val context: Context) : Tool {
    override val name = "send_sms"
    override val description = "Sends an SMS text message to a phone number."

    override val parameterSchema = mapOf(
        "phone_number" to ParamSpec("string", true, "Destination phone number"),
        "message" to ParamSpec("string", true, "Text content"),
    )

    override val requiredPermissions = listOf(PermissionManager.MangalPermission.SEND_SMS)

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val number = args["phone_number"] as? String
            ?: return ToolResult.Failure("phone_number must be a string")
        val message = args["message"] as? String
            ?: return ToolResult.Failure("message must be a string")

        return try {
            @Suppress("DEPRECATION")
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            ToolResult.Success("SMS sent to $number")
        } catch (e: Exception) {
            ToolResult.Failure("SMS send failed: ${e.message}")
        }
    }
}
