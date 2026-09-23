package com.bkpit.mangal.ui.modelmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bkpit.mangal.data.repository.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    onBack: () -> Unit,
    viewModel: ModelManagerViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(rows) { row ->
                ModelRowItem(
                    row = row,
                    fitsInRam = viewModel.canLoad(row.entry),
                    onDownload = { viewModel.download(row.entry) },
                    onCancel = { viewModel.cancelDownload(row.entry) },
                    onDelete = { viewModel.delete(row.entry) },
                    onSetActive = { viewModel.setActive(row.entry) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ModelRowItem(
    row: ModelRow,
    fitsInRam: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(row.entry.displayName, style = MaterialTheme.typography.titleMedium)
        Text(
            "${row.entry.sizeBytes / 1_000_000} MB \u00B7 needs \u2265${row.entry.minRamMb} MB RAM" +
                if (!fitsInRam) " \u2014 likely too big for this device" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (!fitsInRam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        when (val state = row.state) {
            is DownloadState.NotStarted -> {
                Button(onClick = onDownload, enabled = fitsInRam) { Text("Download") }
            }
            is DownloadState.Queued -> Text("Queued\u2026")
            is DownloadState.Downloading -> {
                LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Row {
                    Text("${state.percent}%")
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
            is DownloadState.Complete -> {
                Row {
                    if (row.isActive) {
                        AssistChip(onClick = {}, label = { Text("Active") })
                    } else {
                        Button(onClick = onSetActive) { Text("Use this model") }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
            is DownloadState.Failed -> {
                Text("Failed: ${state.reason}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onDownload) { Text("Retry") }
            }
        }
    }
}
