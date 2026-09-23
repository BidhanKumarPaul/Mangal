package com.bkpit.mangal.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {

            Text("Speech rate", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = settings?.ttsSpeechRate ?: 1.0f,
                onValueChange = { viewModel.setTtsRate(it) },
                valueRange = 0.5f..2.0f
            )

            Spacer(Modifier.height(16.dp))

            Text("Max context length (tokens)", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = (settings?.maxContextLength ?: 2048).toFloat(),
                onValueChange = { viewModel.setMaxContextLength(it.toInt()) },
                valueRange = 512f..4096f,
                steps = 6
            )
            Text("${settings?.maxContextLength ?: 2048} tokens \u2014 lower this on low-RAM/hot devices")

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Wake word (experimental, Phase 5)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings?.wakeWordEnabled ?: false,
                    onCheckedChange = { viewModel.setWakeWordEnabled(it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = { viewModel.clearConversationHistory() }) {
                Text("Clear conversation history")
            }

            Spacer(Modifier.weight(1f))
            Text(
                "developed by BKP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
