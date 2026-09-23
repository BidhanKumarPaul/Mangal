package com.bkpit.mangal.tools

import com.bkpit.mangal.tools.permissions.PermissionManager

/**
 * Phase 4 core: the LLM never executes anything directly. It outputs a JSON
 * object like {"tool": "set_alarm", "args": {"hour": 7, "minute": 30}}. This
 * registry validates that against the tool's declared schema, checks runtime
 * permissions, and only then calls Tool.execute. Anything that fails
 * validation returns a Failure/NeedsPermission the LLM is fed back so it can
 * produce a natural "I can't do that yet" reply instead of silently failing.
 */
class ToolRegistry(private val permissionManager: PermissionManager) {

    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun availableTools(): List<Tool> = tools.values.toList()

    /** Builds the tool-list block injected into the LLM system prompt. */
    fun describeToolsForPrompt(): String = buildString {
        tools.values.forEach { tool ->
            appendLine("- ${tool.name}: ${tool.description}")
            tool.parameterSchema.forEach { (paramName, spec) ->
                val req = if (spec.required) "required" else "optional"
                appendLine("    $paramName (${spec.type}, $req): ${spec.description}")
            }
        }
    }

    suspend fun invoke(toolName: String, rawArgs: Map<String, Any?>): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult.Failure("Unknown tool '$toolName'. Known tools: ${tools.keys}")

        val validation = validate(tool, rawArgs)
        if (validation != null) return ToolResult.Failure(validation)

        val missingPermission = permissionManager.missingFor(tool.requiredPermissions).firstOrNull()
        if (missingPermission != null) return ToolResult.NeedsPermission(missingPermission)

        return runCatching { tool.execute(rawArgs) }
            .getOrElse { e -> ToolResult.Failure("Tool '${tool.name}' threw: ${e.message}") }
    }

    private fun validate(tool: Tool, args: Map<String, Any?>): String? {
        for ((paramName, spec) in tool.parameterSchema) {
            val value = args[paramName]
            if (spec.required && value == null) {
                return "Missing required parameter '$paramName' for tool '${tool.name}'"
            }
            if (value != null && spec.enum != null && value !in spec.enum) {
                return "Parameter '$paramName' must be one of ${spec.enum}, got '$value'"
            }
        }
        return null
    }
}
