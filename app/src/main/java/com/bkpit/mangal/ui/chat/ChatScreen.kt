package com.bkpit.mangal.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bkpit.mangal.data.db.entities.ChatMessageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenModelManager: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    var typedText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result observed next time the tool call is retried */ }

    LaunchedEffect(uiState.neededPermission) {
        uiState.neededPermission?.let { permission ->
            permissionLauncher.launch(permission.manifestPermission)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mangal") },
                actions = {
                    TextButton(onClick = onOpenModelManager) { Text("Models") }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                reverseLayout = false
            ) {
                items(messages) { message -> ChatBubble(message) }
            }

            if (uiState.isThinking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type, or tap the mic") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (typedText.isNotBlank()) {
                        viewModel.sendTypedMessage(typedText)
                        typedText = ""
                    }
                }) { Text("Send") }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { viewModel.startPushToTalk() },
                    enabled = !uiState.isListening && !uiState.isThinking
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Push to talk")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(message.content, modifier = Modifier.padding(12.dp))
        }
    }
}
