package com.bkpit.mangal.tools.impl

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import com.bkpit.mangal.tools.ParamSpec
import com.bkpit.mangal.tools.Tool
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tools.permissions.PermissionManager

/**
 * Since Android 6+/10+, apps cannot programmatically toggle Wi-Fi/Bluetooth
 * state directly (WifiManager.setWifiEnabled is a no-op for 3rd-party apps
 * since API 29) — those two open the relevant Settings panel instead. Volume
 * and flashlight CAN be controlled directly and are handled without any UI
 * hop, which is the "where direct control is restricted" distinction from
 * the spec.
 */
class SettingsToggleTool(private val context: Context) : Tool {
    override val name = "toggle_setting"
    override val description =
        "Toggles a device setting: volume, brightness, flashlight, wifi, or bluetooth."

    override val parameterSchema = mapOf(
        "setting" to ParamSpec(
            "string", true, "Which setting to change",
            enum = listOf("volume", "flashlight", "wifi", "bluetooth")
        ),
        "state" to ParamSpec(
            "string", false, "on/off for flashlight/wifi/bluetooth",
            enum = listOf("on", "off")
        ),
        "level_percent" to ParamSpec("number", false, "0-100, for volume"),
    )

    override val requiredPermissions: List<PermissionManager.MangalPermission> = emptyList()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return when (args["setting"] as? String) {
            "volume" -> setVolume(args)
            "flashlight" -> setFlashlight(args)
            "wifi" -> openPanel(Settings.Panel.ACTION_WIFI, "Wi-Fi")
            "bluetooth" -> openPanel(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth")
            else -> ToolResult.Failure("Unknown or missing 'setting'")
        }
    }

    private fun setVolume(args: Map<String, Any?>): ToolResult {
        val percent = (args["level_percent"] as? Number)?.toInt()
            ?: return ToolResult.Failure("volume needs level_percent (0-100)")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (percent.coerceIn(0, 100) / 100f * max).toInt()
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return ToolResult.Success("Volume set to $percent%")
    }

    private fun setFlashlight(args: Map<String, Any?>): ToolResult {
        val on = (args["state"] as? String) == "on"
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ToolResult.Failure("No flash-capable camera found")
            cameraManager.setTorchMode(cameraId, on)
            ToolResult.Success("Flashlight turned ${if (on) "on" else "off"}")
        } catch (e: Exception) {
            ToolResult.Failure("Flashlight toggle failed: ${e.message}")
        }
    }

    private fun openPanel(action: String, label: String): ToolResult {
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Opened $label settings — direct toggling isn't allowed by Android for 3rd-party apps")
        } else {
            ToolResult.Failure("Could not open $label settings panel")
        }
    }
}
