package com.rr.aido.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditPromptDialog(
    title: String,
    currentPrompt: String,
    defaultPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var prompt by remember { mutableStateOf(currentPrompt.ifEmpty { defaultPrompt }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "Edit the instructions for the AI. Keep it clear and concise.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Make it tall for multi-line editing
                    label = { Text("Prompt Instructions") },
                    placeholder = { Text("Enter instructions here...") },
                    minLines = 10,
                    maxLines = 20
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { prompt = defaultPrompt },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Default")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(prompt)
                    onDismiss()
                }
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
