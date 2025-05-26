package uk.co.effectivecode.firebender.splitbill.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.effectivecode.firebender.splitbill.data.EditableReceipt
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem

@Composable
fun ReceiptScreen(
    editableReceipt: EditableReceipt,
    onAddItem: (ReceiptItem) -> Unit,
    onUpdateItem: (Int, ReceiptItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onUpdateServiceCharge: (Double) -> Unit,
    onUpdateTotal: (Double) -> Unit,
    onEnterSplittingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Prevent FAB from obscuring content
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Receipt Parsed Successfully",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onEnterSplittingMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Split Bill")
                    }
                }
            }
        }
        
        // Error display
        editableReceipt.originalResult.error?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        // Items header
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            onAddItem(ReceiptItem("New Item", 1, 0.0))
                        }
                    ) {
                        Text("Add Item")
                    }
                }
            }
        }
        
        // Items list
        itemsIndexed(editableReceipt.items) { index, item ->
            EditableReceiptItem(
                item = item,
                onItemUpdated = { updatedItem -> onUpdateItem(index, updatedItem) },
                onItemDeleted = { onDeleteItem(index) }
            )
        }
        
        // Subtotal
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal (Items)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "£%.2f".format(editableReceipt.calculation.subtotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Service charge
        item {
            EditableServiceCharge(
                service = editableReceipt.serviceCharge,
                onServiceUpdated = onUpdateServiceCharge
            )
        }
        
        // Total
        item {
            EditableTotal(
                total = editableReceipt.total,
                expectedTotal = editableReceipt.calculation.expectedTotal,
                hasDiscrepancy = editableReceipt.calculation.hasDiscrepancy,
                onTotalUpdated = onUpdateTotal
            )
        }
        
        // Discrepancy warning
        if (editableReceipt.calculation.hasDiscrepancy) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ Total Discrepancy Detected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Expected: £%.2f, Actual: £%.2f (Difference: £%.2f)".format(
                                editableReceipt.calculation.expectedTotal,
                                editableReceipt.calculation.actualTotal,
                                editableReceipt.calculation.discrepancyAmount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processing receipt...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Split Bill",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tap the + button to add a receipt photo",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EditableServiceCharge(
    service: Double,
    onServiceUpdated: (Double) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedService by remember { mutableStateOf("%.2f".format(service)) }
    
    LaunchedEffect(service) {
        editedService = "%.2f".format(service)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = editedService,
                    onValueChange = { editedService = it },
                    label = { Text("Service Charge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            isEditing = false
                            editedService = "%.2f".format(service)
                        }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val newService = editedService.toDoubleOrNull() ?: service
                            onServiceUpdated(newService)
                            isEditing = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Charge",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "£%.2f".format(service),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Service Charge"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditableTotal(
    total: Double,
    expectedTotal: Double,
    hasDiscrepancy: Boolean,
    onTotalUpdated: (Double) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTotal by remember { mutableStateOf("%.2f".format(total)) }
    
    LaunchedEffect(total) {
        editedTotal = "%.2f".format(total)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasDiscrepancy) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = editedTotal,
                    onValueChange = { editedTotal = it },
                    label = { Text("Total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            isEditing = false
                            editedTotal = "%.2f".format(total)
                        }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val newTotal = editedTotal.toDoubleOrNull() ?: total
                            onTotalUpdated(newTotal)
                            isEditing = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasDiscrepancy) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "£%.2f".format(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDiscrepancy) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Total",
                            tint = if (hasDiscrepancy) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditableReceiptItem(
    item: ReceiptItem,
    onItemUpdated: (ReceiptItem) -> Unit,
    onItemDeleted: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(item.name) }
    var editedQuantity by remember { mutableStateOf(item.quantity.toString()) }
    var editedCost by remember { mutableStateOf("%.2f".format(item.cost)) }
    
    // Reset editing state when item changes
    LaunchedEffect(item) {
        editedName = item.name
        editedQuantity = item.quantity.toString()
        editedCost = "%.2f".format(item.cost)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = editedQuantity,
                        onValueChange = { editedQuantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = editedCost,
                        onValueChange = { editedCost = it },
                        label = { Text("Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onItemDeleted,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    Row {
                        TextButton(
                            onClick = {
                                isEditing = false
                                editedName = item.name
                                editedQuantity = item.quantity.toString()
                                editedCost = "%.2f".format(item.cost)
                            }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                val quantity = editedQuantity.toIntOrNull() ?: item.quantity
                                val cost = editedCost.toDoubleOrNull() ?: item.cost
                                val updatedItem = item.copy(
                                    name = editedName.trim(),
                                    quantity = quantity,
                                    cost = cost
                                )
                                onItemUpdated(updatedItem)
                                isEditing = false
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.quantity > 1) {
                        Text(
                            text = "Quantity: ${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "£%.2f".format(item.cost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Item"
                        )
                    }
                }
            }
        }
    }
}
