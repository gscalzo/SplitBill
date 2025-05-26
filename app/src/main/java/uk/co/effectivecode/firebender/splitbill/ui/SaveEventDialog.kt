package uk.co.effectivecode.firebender.splitbill.ui

import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import uk.co.effectivecode.firebender.splitbill.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveEventDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Log.d(TAG, "SaveEventDialog composing")
    var eventName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_event)) },
        text = {
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text(stringResource(R.string.event_name)) },
                placeholder = { Text(stringResource(R.string.enter_event_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    Log.d(TAG, "Save button clicked with event name: '${eventName.trim()}'")
                    onSave(eventName.trim()) 
                },
                enabled = eventName.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                Log.d(TAG, "Cancel button clicked")
                onDismiss() 
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private const val TAG = "SaveEventDialog"
