package com.bkpit.mangal.tools

import com.bkpit.mangal.tools.permissions.PermissionManager

/**
 * One callable action the LLM can invoke. The LLM never parses free-form
 * intent text — it must emit a JSON object matching [name] and [parameters],
 * validated by [ToolRegistry] against [parameterSchema] before [execute] runs.
 */
interface Tool {
    /** Stable identifier the LLM uses in its tool_call JSON, e.g. "set_alarm". */
    val name: String

    /** One-line natural-language description injected into the system prompt. */
    val description: String

    /** JSON-schema-like parameter description shown to the LLM (see ToolCallSchema). */
    val parameterSchema: Map<String, ParamSpec>

    /** Permissions that must be granted before this tool may run. */
    val requiredPermissions: List<PermissionManager.MangalPermission>

    /** Runs the action. [args] have already been validated against [parameterSchema]. */
    suspend fun execute(args: Map<String, Any?>): ToolResult
}

data class ParamSpec(
    val type: String,          // "string" | "number" | "boolean"
    val required: Boolean = true,
    val description: String = "",
    val enum: List<String>? = null
)

sealed class ToolResult {
    data class Success(val message: String) : ToolResult()
    data class Failure(val reason: String) : ToolResult()
    data class NeedsPermission(val permission: PermissionManager.MangalPermission) : ToolResult()
}
