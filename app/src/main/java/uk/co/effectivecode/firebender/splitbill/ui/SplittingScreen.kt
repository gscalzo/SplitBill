package uk.co.effectivecode.firebender.splitbill.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.effectivecode.firebender.splitbill.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplittingScreen(
    receiptWithSplitting: EditableReceiptWithSplitting,
    onAddParticipant: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onAssignItemToParticipant: (Int, String) -> Unit,
    onAssignItemToEqualSplit: (Int, List<String>) -> Unit,
    onUpdateItem: (Int, ReceiptItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onShowBalanceSummary: () -> Unit,
    onExitSplitting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = receiptWithSplitting.billSplitSummary
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExitSplitting) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Split Bill",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onShowBalanceSummary,
                    enabled = summary.participants.isNotEmpty()
                ) {
                    Text("Summary")
                }
            }
        }
        
        // Participants Section
        item {
            ParticipantsSection(
                participants = summary.participants,
                onAddParticipant = onAddParticipant,
                onRemoveParticipant = onRemoveParticipant
            )
        }
        
        // Assignment Status
        if (summary.participants.isNotEmpty()) {
            item {
                AssignmentStatusCard(summary = summary)
            }
        }
        
        // Items Section
        item {
            Text(
                text = "Items Assignment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        itemsIndexed(summary.assignedItems) { index, assignedItem ->
            AssignableReceiptItem(
                assignedItem = assignedItem,
                participants = summary.participants,
                onAssignToParticipant = { participantId ->
                    onAssignItemToParticipant(index, participantId)
                },
                onAssignToEqualSplit = { participantIds ->
                    onAssignItemToEqualSplit(index, participantIds)
                },
                onUpdateItem = { newItem ->
                    onUpdateItem(index, newItem)
                },
                onDeleteItem = {
                    onDeleteItem(index)
                }
            )
        }
    }
}

@Composable
fun ParticipantsSection(
    participants: List<Participant>,
    onAddParticipant: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "Participants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            
            if (participants.isEmpty()) {
                Text(
                    text = "Add participants to split the bill",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = participant.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = { onRemoveParticipant(participant.id) }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove ${participant.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddParticipantDialog(
            onDismiss = { showAddDialog = false },
            onAddParticipant = { name ->
                onAddParticipant(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddParticipantDialog(
    onDismiss: () -> Unit,
    onAddParticipant: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    val handleAddParticipant = {
        if (name.trim().isNotEmpty()) {
            onAddParticipant(name.trim())
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Participant") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleAddParticipant() }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = handleAddParticipant,
                enabled = name.trim().isNotEmpty()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssignmentStatusCard(summary: BillSplitSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isFullyAssigned) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Assignment Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (summary.isFullyAssigned) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Assigned: £%.2f".format(summary.totalAssigned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (summary.isFullyAssigned) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (summary.totalUnassigned > 0) {
                        Text(
                            text = "Unassigned: £%.2f".format(summary.totalUnassigned),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (summary.isFullyAssigned) {
                    Text(
                        text = "✓ Complete",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignableReceiptItem(
    assignedItem: AssignedReceiptItem,
    participants: List<Participant>,
    onAssignToParticipant: (String) -> Unit,
    onAssignToEqualSplit: (List<String>) -> Unit,
    onUpdateItem: (ReceiptItem) -> Unit,
    onDeleteItem: () -> Unit
) {
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (assignedItem.assignment) {
                is ItemAssignment.Unassigned -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isEditing) {
                EditableReceiptItem(
                    item = assignedItem.receiptItem,
                    onItemUpdated = { updatedItem ->
                        onUpdateItem(updatedItem)
                        isEditing = false
                    },
                    onItemDeleted = {
                        onDeleteItem()
                        isEditing = false
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = assignedItem.receiptItem.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (assignedItem.receiptItem.quantity > 1) {
                            Text(
                                text = "Quantity: ${assignedItem.receiptItem.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "£%.2f".format(assignedItem.receiptItem.cost),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Assignment status
                        when (val assignment = assignedItem.assignment) {
                            is ItemAssignment.Unassigned -> {
                                Text(
                                    text = "⚠️ Unassigned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ItemAssignment.IndividualAssignment -> {
                                val participant = participants.find { it.id == assignment.participantId }
                                Text(
                                    text = "→ ${participant?.name ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ItemAssignment.EqualSplit -> {
                                val participantNames = participants
                                    .filter { assignment.participantIds.contains(it.id) }
                                    .map { it.name }
                                Text(
                                    text = "⚖️ Split: ${participantNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Column {
                        TextButton(
                            onClick = { showAssignmentDialog = true },
                            enabled = participants.isNotEmpty()
                        ) {
                            Text("Assign")
                        }
                        TextButton(
                            onClick = { isEditing = true }
                        ) {
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
    
    if (showAssignmentDialog) {
        AssignmentDialog(
            item = assignedItem.receiptItem,
            participants = participants,
            currentAssignment = assignedItem.assignment,
            onDismiss = { showAssignmentDialog = false },
            onAssignToParticipant = { participantId ->
                onAssignToParticipant(participantId)
                showAssignmentDialog = false
            },
            onAssignToEqualSplit = { participantIds ->
                onAssignToEqualSplit(participantIds)
                showAssignmentDialog = false
            }
        )
    }
}

@Composable
fun AssignmentDialog(
    item: ReceiptItem,
    participants: List<Participant>,
    currentAssignment: ItemAssignment,
    onDismiss: () -> Unit,
    onAssignToParticipant: (String) -> Unit,
    onAssignToEqualSplit: (List<String>) -> Unit
) {
    var selectedOption by remember { mutableStateOf("individual") }
    var selectedParticipant by remember { mutableStateOf("") }
    var selectedParticipants by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(currentAssignment) {
        when (currentAssignment) {
            is ItemAssignment.IndividualAssignment -> {
                selectedOption = "individual"
                selectedParticipant = currentAssignment.participantId
            }
            is ItemAssignment.EqualSplit -> {
                selectedOption = "split"
                selectedParticipants = currentAssignment.participantIds.toSet()
            }
            ItemAssignment.Unassigned -> {
                selectedOption = "individual"
                selectedParticipant = ""
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign: ${item.name}") },
        text = {
            Column {
                // Individual assignment option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedOption == "individual",
                            onClick = { selectedOption = "individual" }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "individual",
                        onClick = { selectedOption = "individual" }
                    )
                    Text("Assign to one person")
                }
                
                if (selectedOption == "individual") {
                    participants.forEach { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedParticipant == participant.id,
                                    onClick = { selectedParticipant = participant.id }
                                )
                                .padding(vertical = 4.dp, horizontal = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedParticipant == participant.id,
                                onClick = { selectedParticipant = participant.id }
                            )
                            Text(participant.name)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Equal split option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedOption == "split",
                            onClick = { selectedOption = "split" }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "split",
                        onClick = { selectedOption = "split" }
                    )
                    Text("Split equally")
                }
                
                if (selectedOption == "split") {
                    participants.forEach { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedParticipants.contains(participant.id),
                                onCheckedChange = { checked ->
                                    selectedParticipants = if (checked) {
                                        selectedParticipants + participant.id
                                    } else {
                                        selectedParticipants - participant.id
                                    }
                                }
                            )
                            Text(participant.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedOption) {
                        "individual" -> {
                            if (selectedParticipant.isNotEmpty()) {
                                onAssignToParticipant(selectedParticipant)
                            }
                        }
                        "split" -> {
                            onAssignToEqualSplit(selectedParticipants.toList())
                        }
                    }
                },
                enabled = when (selectedOption) {
                    "individual" -> selectedParticipant.isNotEmpty()
                    "split" -> selectedParticipants.isNotEmpty()
                    else -> false
                }
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
