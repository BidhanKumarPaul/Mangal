package com.bkpit.mangal.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkpit.mangal.data.repository.ChatRepository
import com.bkpit.mangal.llm.LlamaEngine
import com.bkpit.mangal.llm.ParsedModelOutput
import com.bkpit.mangal.llm.SystemPrompt
import com.bkpit.mangal.llm.ToolCallParser
import com.bkpit.mangal.stt.AudioCapture
import com.bkpit.mangal.stt.WhisperEngine
import com.bkpit.mangal.tools.ToolRegistry
import com.bkpit.mangal.tools.ToolResult
import com.bkpit.mangal.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val lastTranscript: String = "",
    val lastReply: String = "",
    val error: String? = null,
    /** Set when a tool needs a permission the user hasn't granted yet — the
     *  screen shows a request button for exactly this permission. */
    val neededPermission: com.bkpit.mangal.tools.permissions.PermissionManager.MangalPermission? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val llamaEngine: LlamaEngine,
    private val whisperEngine: WhisperEngine,
    private val ttsManager: TtsManager,
    private val toolRegistry: ToolRegistry,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages = chatRepository.observeMessages()

    /** Push-to-talk entry point (Phase 3): capture one chunk, transcribe, run the loop. */
    fun startPushToTalk() {
        _uiState.value = _uiState.value.copy(isListening = true, error = null)
        viewModelScope.launch {
            runCatching {
                val chunk = AudioCapture.captureChunks(chunkDurationMs = 4000).first()
                check(whisperEngine.isLoaded) { "No STT model loaded — set one up in Model Manager" }
                whisperEngine.transcribe(chunk)
            }.onSuccess { transcript ->
                _uiState.value = _uiState.value.copy(isListening = false, lastTranscript = transcript)
                handleUserInput(transcript)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isListening = false, error = e.message)
            }
        }
    }

    /** Also callable directly from a text field, for testing without a mic. */
    fun sendTypedMessage(text: String) {
        viewModelScope.launch { handleUserInput(text) }
    }

    private suspend fun handleUserInput(userText: String) {
        if (userText.isBlank()) return
        chatRepository.addMessage(role = "user", content = userText)
        _uiState.value = _uiState.value.copy(isThinking = true, error = null, neededPermission = null)

        val historyBlock = chatRepository.recent(10).reversed().joinToString("\n") { "${it.role}: ${it.content}" }
        val systemPrompt = SystemPrompt.build(toolRegistry.describeToolsForPrompt())
        val fullPrompt = "$systemPrompt\n\n$historyBlock\nuser: $userText\nassistant:"

        val rawOutput = runCatching {
            check(llamaEngine.isLoaded) { "No LLM loaded — set one up in Model Manager" }
            llamaEngine.complete(fullPrompt)
        }.getOrElse { e ->
            _uiState.value = _uiState.value.copy(isThinking = false, error = e.message)
            return
        }

        when (val parsed = ToolCallParser.parse(rawOutput)) {
            is ParsedModelOutput.PlainReply -> finishTurn(parsed.text)

            is ParsedModelOutput.ToolCall -> {
                chatRepository.addMessage(role = "assistant", content = "[calling ${parsed.toolName}]", toolName = parsed.toolName)
                when (val result = toolRegistry.invoke(parsed.toolName, parsed.args)) {
                    is ToolResult.Success -> {
                        // Feed the tool result back so the model can phrase a
                        // natural confirmation, rather than us hard-coding one.
                        val confirmPrompt = "$fullPrompt $rawOutput\ntool_result: ${result.message}\nassistant:"
                        val confirmation = runCatching { llamaEngine.complete(confirmPrompt, maxTokens = 80) }
                            .getOrDefault(result.message)
                        finishTurn(confirmation)
                    }
                    is ToolResult.Failure -> finishTurn("I couldn't do that: ${result.reason}")
                    is ToolResult.NeedsPermission -> {
                        _uiState.value = _uiState.value.copy(isThinking = false, neededPermission = result.permission)
                        finishTurn("I need a permission for that first.", speak = false)
                    }
                }
            }
        }
    }

    private suspend fun finishTurn(reply: String, speak: Boolean = true) {
        chatRepository.addMessage(role = "assistant", content = reply)
        _uiState.value = _uiState.value.copy(isThinking = false, lastReply = reply)
        if (speak) runCatching { ttsManager.speakAndWait(reply) }
    }

    fun clearHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
