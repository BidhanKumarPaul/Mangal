package com.bkpit.mangal.llm

import org.json.JSONObject

/**
 * Parses the model's raw text output looking for a tool-call JSON object.
 * We deliberately do NOT require the whole response to be JSON — small
 * instruct models often wrap it in a sentence or code fence — so we scan for
 * the first top-level {...} block and try to parse just that.
 *
 * Expected shape:
 *   {"tool": "set_alarm", "args": {"hour": 7, "minute": 30}}
 * A plain-conversation reply (no tool needed) has no such block at all.
 */
sealed class ParsedModelOutput {
    data class ToolCall(val toolName: String, val args: Map<String, Any?>) : ParsedModelOutput()
    data class PlainReply(val text: String) : ParsedModelOutput()
}

object ToolCallParser {

    fun parse(rawModelOutput: String): ParsedModelOutput {
        val jsonBlock = extractFirstJsonObject(rawModelOutput) ?: return ParsedModelOutput.PlainReply(rawModelOutput.trim())

        return runCatching {
            val obj = JSONObject(jsonBlock)
            val toolName = obj.optString("tool", "")
            if (toolName.isBlank()) return@runCatching ParsedModelOutput.PlainReply(rawModelOutput.trim())

            val argsObj = obj.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, Any?>()
            argsObj.keys().forEach { key -> args[key] = argsObj.get(key) }

            ParsedModelOutput.ToolCall(toolName, args)
        }.getOrElse {
            ParsedModelOutput.PlainReply(rawModelOutput.trim())
        }
    }

    /** Finds the first balanced {...} substring, tolerating nested braces. */
    private fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null

        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null // unbalanced — treat as plain text
    }
}
