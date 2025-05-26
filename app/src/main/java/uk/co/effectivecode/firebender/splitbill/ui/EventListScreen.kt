package uk.co.effectivecode.firebender.splitbill.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.effectivecode.firebender.splitbill.R
import uk.co.effectivecode.firebender.splitbill.data.BillEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    events: List<BillEvent>,
    onEventClick: (String) -> Unit,
    onCreateNewBill: () -> Unit,
    onDeleteEvent: (String) -> Unit,
    onEditEventName: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "EventListScreen composing with ${events.size} events")
    events.forEach { event ->
        Log.d(TAG, "Rendering event: ${event.id}, name: ${event.name}")
    }
    
    var eventToEdit by remember { mutableStateOf<BillEvent?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BillEvent?>(null) }
    
    Column(modifier = modifier.fillMaxSize()) {
        if (events.isEmpty()) {
            Log.d(TAG, "Displaying welcome screen - no events")
            WelcomeScreen(modifier = Modifier.weight(1f))
        } else {
            Log.d(TAG, "Displaying event list with ${events.size} events")
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        onEventClick = { onEventClick(event.id) },
                        onEditName = { eventToEdit = event },
                        onDelete = { showDeleteDialog = event }
                    )
                }
            }
        }
        
        // Floating Action Button for creating new bill
        FloatingActionButton(
            onClick = onCreateNewBill,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New Bill")
        }
    }
    
    // Edit Event Name Dialog
    eventToEdit?.let { event ->
        EditEventNameDialog(
            currentName = event.name,
            onConfirm = { newName ->
                onEditEventName(event.id, newName)
                eventToEdit = null
            },
            onDismiss = { eventToEdit = null }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_event_title)) },
            text = { Text(stringResource(R.string.delete_event_confirmation, event.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEvent(event.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete_event_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: BillEvent,
    onEventClick: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEventClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEditName) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_name))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_event_action))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatDate(event.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Event summary info
            val totalAmount = event.receiptWithSplitting.editableReceipt.total
            val participantCount = event.receiptWithSplitting.participants.size
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.total_amount, totalAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = if (participantCount == 1) {
                        stringResource(R.string.participant_count, participantCount)
                    } else {
                        stringResource(R.string.participant_count_plural, participantCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventNameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_event_name_title)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.event_name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private const val TAG = "EventListScreen"
