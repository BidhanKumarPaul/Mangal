package com.bkpit.mangal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkpit.mangal.data.db.AppDatabase
import com.bkpit.mangal.data.db.entities.SettingsEntity
import com.bkpit.mangal.data.repository.ChatRepository
import com.bkpit.mangal.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val db: AppDatabase,
    private val chatRepository: ChatRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    val settings: StateFlow<SettingsEntity?> =
        db.settingsDao().observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setTtsRate(rate: Float) {
        ttsManager.setRate(rate)
        viewModelScope.launch {
            val current = db.settingsDao().getOnce() ?: SettingsEntity()
            db.settingsDao().upsert(current.copy(ttsSpeechRate = rate))
        }
    }

    fun setMaxContextLength(length: Int) {
        viewModelScope.launch {
            val current = db.settingsDao().getOnce() ?: SettingsEntity()
            db.settingsDao().upsert(current.copy(maxContextLength = length))
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = db.settingsDao().getOnce() ?: SettingsEntity()
            db.settingsDao().upsert(current.copy(wakeWordEnabled = enabled))
        }
    }

    /** Phase 5: conversation memory must be user-clearable. */
    fun clearConversationHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }
}
