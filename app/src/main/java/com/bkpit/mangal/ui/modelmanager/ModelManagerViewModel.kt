package com.bkpit.mangal.ui.modelmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkpit.mangal.data.db.AppDatabase
import com.bkpit.mangal.data.db.entities.SettingsEntity
import com.bkpit.mangal.data.download.ModelCatalog
import com.bkpit.mangal.data.download.ModelCatalogEntry
import com.bkpit.mangal.data.repository.DownloadState
import com.bkpit.mangal.data.repository.ModelDownloadRepository
import com.bkpit.mangal.llm.LlamaEngine
import com.bkpit.mangal.stt.WhisperEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelRow(
    val entry: ModelCatalogEntry,
    val state: DownloadState,
    val isActive: Boolean
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val downloadRepository: ModelDownloadRepository,
    private val db: AppDatabase,
    private val llamaEngine: LlamaEngine,
    private val whisperEngine: WhisperEngine,
) : ViewModel() {

    private val activeSettings: StateFlow<SettingsEntity?> =
        db.settingsDao().observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val perModelState = ModelCatalog.entries.associate { entry ->
        entry.id to downloadRepository.observeProgress(entry.id)
    }

    val rows: StateFlow<List<ModelRow>> = combine(
        activeSettings,
        combine(perModelState.values.toList()) { it.toList() }
    ) { settings, states ->
        ModelCatalog.entries.mapIndexed { index, entry ->
            val isActive = entry.id == settings?.activeLlmModelFileName ||
                entry.id == settings?.activeWhisperModelFileName
            ModelRow(entry, states[index], isActive)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Phase 5: refuse to load a model too big for this device's RAM. */
    fun availableRamMb(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / (1024 * 1024)
    }

    fun canLoad(entry: ModelCatalogEntry): Boolean = entry.minRamMb <= availableRamMb()

    fun download(entry: ModelCatalogEntry) {
        if (!canLoad(entry)) return // UI should already be disabling the button; belt & suspenders
        downloadRepository.enqueueDownload(entry.id)
    }

    fun cancelDownload(entry: ModelCatalogEntry) = downloadRepository.cancelDownload(entry.id)

    fun delete(entry: ModelCatalogEntry) {
        viewModelScope.launch {
            if (entry.id == activeSettings.value?.activeLlmModelFileName) llamaEngine.unload()
            if (entry.id == activeSettings.value?.activeWhisperModelFileName) whisperEngine.unload()
            downloadRepository.delete(entry.id)
        }
    }

    fun setActive(entry: ModelCatalogEntry) {
        viewModelScope.launch {
            val file = downloadRepository.localFileFor(entry.id) ?: return@launch
            val current = db.settingsDao().getOnce() ?: SettingsEntity()

            when (entry.kind) {
                com.bkpit.mangal.data.download.ModelKind.LLM -> {
                    llamaEngine.unload()
                    llamaEngine.load(file, contextLength = current.maxContextLength)
                    db.settingsDao().upsert(current.copy(activeLlmModelFileName = entry.id))
                }
                com.bkpit.mangal.data.download.ModelKind.WHISPER -> {
                    whisperEngine.unload()
                    whisperEngine.load(file)
                    db.settingsDao().upsert(current.copy(activeWhisperModelFileName = entry.id))
                }
            }
        }
    }
}
