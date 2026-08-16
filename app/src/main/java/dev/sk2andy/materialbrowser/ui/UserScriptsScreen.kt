package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.data.UserScript
import dev.sk2andy.materialbrowser.data.UserScriptRunAt
import dev.sk2andy.materialbrowser.data.UserScriptStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptsScreen(
    store: UserScriptStore,
    onNavigateBack: () -> Unit
) {
    val scripts by store.scriptsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var editingScript by remember { mutableStateOf<UserScript?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Scripts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingScript = UserScript(name = "", code = "")
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Script")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            if (scripts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No scripts added yet.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            items(scripts) { script ->
                ListItem(
                    headlineContent = { Text(script.name) },
                    supportingContent = { Text(script.urlPattern) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = script.isEnabled,
                                onCheckedChange = { store.toggleScript(script.id, it) }
                            )
                            IconButton(onClick = { store.deleteScript(script.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        editingScript = script
                        showEditor = true
                    }
                )
                Divider()
            }
        }
    }

    if (showEditor && editingScript != null) {
        UserScriptEditorDialog(
            initialScript = editingScript!!,
            onDismiss = { showEditor = false },
            onSave = { updatedScript ->
                store.addOrUpdateScript(updatedScript)
                showEditor = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptEditorDialog(
    initialScript: UserScript,
    onDismiss: () -> Unit,
    onSave: (UserScript) -> Unit
) {
    var name by remember { mutableStateOf(initialScript.name) }
    var pattern by remember { mutableStateOf(initialScript.urlPattern) }
    var code by remember { mutableStateOf(initialScript.code) }
    var runAt by remember { mutableStateOf(initialScript.runAt) }
    
    var showRunAtMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialScript.name.isEmpty()) "New Script" else "Edit Script") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("URL Match Pattern (e.g. *://*.youtube.com/*)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = showRunAtMenu,
                    onExpandedChange = { showRunAtMenu = !showRunAtMenu }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = if (runAt == UserScriptRunAt.DOCUMENT_START) "Document Start" else "Document End",
                        onValueChange = { },
                        label = { Text("Run At") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRunAtMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showRunAtMenu,
                        onDismissRequest = { showRunAtMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Document Start") },
                            onClick = { runAt = UserScriptRunAt.DOCUMENT_START; showRunAtMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Document End") },
                            onClick = { runAt = UserScriptRunAt.DOCUMENT_END; showRunAtMenu = false }
                        )
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("JavaScript Code") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = initialScript.copy(
                        name = name.ifEmpty { "Untitled" },
                        urlPattern = pattern.ifEmpty { "*" },
                        code = code,
                        runAt = runAt
                    )
                    onSave(updated)
                },
                enabled = code.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
