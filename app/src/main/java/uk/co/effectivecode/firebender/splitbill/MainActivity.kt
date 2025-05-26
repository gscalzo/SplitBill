package uk.co.effectivecode.firebender.splitbill

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.launch
import uk.co.effectivecode.firebender.splitbill.data.*
import uk.co.effectivecode.firebender.splitbill.service.OpenAIService
import uk.co.effectivecode.firebender.splitbill.service.ReceiptParsingService
import uk.co.effectivecode.firebender.splitbill.ui.theme.SplitBillTheme
import uk.co.effectivecode.firebender.splitbill.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")
        setContent {
            SplitBillTheme {
                MainApp()
            }
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    Log.d(TAG, "MainApp composing")
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Create ViewModel with dependency injection
    val receiptService: ReceiptParsingService = remember { OpenAIService(useMock = false) }
    val eventRepository: EventRepository = remember { FileEventRepository(context) }
    val viewModel: ReceiptViewModel = viewModel { ReceiptViewModel(receiptService, eventRepository) }
    
    // Create a temporary file for camera capture
    val photoFile = remember {
        File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
    }
    
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.parseReceipt(context, photoUri)
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.parseReceipt(context, it) }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        }
    }
    
    val mainScreenState by viewModel.mainScreenState.collectAsState()
    Log.d(TAG, "MainApp current screen state: ${mainScreenState::class.simpleName}")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = when (val currentState = mainScreenState) {
                        is MainScreenState.EventList -> {
                            Log.d(TAG, "Displaying EventList top bar")
                            stringResource(id = R.string.app_name)
                        }
                        is MainScreenState.CreateNewBill -> {
                            Log.d(TAG, "Displaying CreateNewBill top bar")
                            stringResource(id = R.string.new_bill)
                        }
                        is MainScreenState.ViewEvent -> {
                            Log.d(TAG, "Displaying ViewEvent top bar for event: ${currentState.eventId}")
                            stringResource(id = R.string.event_details)
                        }
                    })
                }
            )
        }
    ) { innerPadding ->
        when (val currentState = mainScreenState) {
            is MainScreenState.EventList -> {
                Log.d(TAG, "Rendering EventList screen")
                EventListContent(
                    viewModel = viewModel,
                    onCreateNewBill = {
                        Log.d(TAG, "Create new bill clicked")
                        viewModel.navigateToCreateNewBill()
                        showImageSourceDialog = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is MainScreenState.CreateNewBill -> {
                Log.d(TAG, "Rendering CreateNewBill screen")
                CreateNewBillContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is MainScreenState.ViewEvent -> {
                Log.d(TAG, "Rendering ViewEvent screen for event: ${currentState.eventId}")
                ViewEventContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        
        // Image source chooser dialog
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text(stringResource(R.string.add_receipt)) },
                text = { Text(stringResource(R.string.choose_how_to_add_receipt)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    cameraLauncher.launch(photoUri)
                                }
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.take_photo))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text(stringResource(R.string.choose_from_gallery))
                    }
                }
            )
        }
    }
}

@Composable
fun EventListContent(
    viewModel: ReceiptViewModel,
    onCreateNewBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventList by viewModel.eventList.collectAsState()
    Log.d(TAG, "EventListContent composing with ${eventList.size} events")
    
    EventListScreen(
        events = eventList,
        onEventClick = { eventId -> 
            Log.d(TAG, "Event clicked: $eventId")
            viewModel.navigateToViewEvent(eventId) 
        },
        onCreateNewBill = onCreateNewBill,
        onDeleteEvent = { eventId -> 
            Log.d(TAG, "Delete event clicked: $eventId")
            viewModel.deleteEvent(eventId) 
        },
        onEditEventName = { eventId, newName -> 
            Log.d(TAG, "Edit event name clicked: $eventId -> $newName")
            viewModel.updateEventName(eventId, newName) 
        },
        modifier = modifier
    )
}

@Composable
fun CreateNewBillContent(
    viewModel: ReceiptViewModel,
    modifier: Modifier = Modifier
) {
    val receiptUiState by viewModel.receiptUiState.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = receiptUiState) {
            is ReceiptUiState.Initial -> {
                WelcomeScreen(modifier = Modifier.padding(16.dp))
            }
            
            is ReceiptUiState.Loading -> {
                LoadingScreen(modifier = Modifier.padding(16.dp))
            }
            
            is ReceiptUiState.Error -> {
                ErrorScreen(
                    message = currentState.message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            is ReceiptUiState.Success -> {
                ReceiptScreen(
                    editableReceipt = currentState.editableReceipt,
                    onAddItem = { item -> viewModel.addItem(item) },
                    onUpdateItem = { index, item -> viewModel.updateItem(index, item) },
                    onDeleteItem = { index -> viewModel.deleteItem(index) },
                    onUpdateServiceCharge = { serviceCharge -> viewModel.updateServiceCharge(serviceCharge) },
                    onUpdateTotal = { total -> viewModel.updateTotal(total) },
                    onEnterSplittingMode = { viewModel.enterSplittingMode() },
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            is ReceiptUiState.SplittingMode -> {
                SplittingScreen(
                    receiptWithSplitting = currentState.receiptWithSplitting,
                    onAddParticipant = { name -> viewModel.addParticipant(name) },
                    onRemoveParticipant = { participantId -> viewModel.removeParticipant(participantId) },
                    onAssignItemToParticipant = { itemIndex, participantId -> 
                        viewModel.assignItemToParticipant(itemIndex, participantId) 
                    },
                    onAssignItemToEqualSplit = { itemIndex, participantIds -> 
                        viewModel.assignItemToEqualSplit(itemIndex, participantIds) 
                    },
                    onUpdateItem = { index, item -> viewModel.updateItem(index, item) },
                    onDeleteItem = { index -> viewModel.deleteItem(index) },
                    onShowBalanceSummary = { viewModel.showBalanceSummary() },
                    onExitSplitting = { viewModel.exitSplittingMode() }
                )
            }
            
            is ReceiptUiState.BalanceSummary -> {
                BalanceSummaryScreen(
                    receiptWithSplitting = currentState.receiptWithSplitting,
                    onBackToSplitting = { viewModel.backToSplitting() },
                    onExitSplitting = { viewModel.exitSplittingMode() },
                    onDesignatePayer = { payerId -> viewModel.designatePayer(payerId) },
                    onClearPayer = { viewModel.clearPayer() },
                    onSaveEvent = { eventName -> viewModel.saveCurrentBillAsEvent(eventName) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ViewEventContent(
    viewModel: ReceiptViewModel,
    modifier: Modifier = Modifier
) {
    val receiptUiState by viewModel.receiptUiState.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = receiptUiState) {
            is ReceiptUiState.BalanceSummary -> {
                BalanceSummaryScreen(
                    receiptWithSplitting = currentState.receiptWithSplitting,
                    onBackToSplitting = { /* Not applicable for viewing events */ },
                    onExitSplitting = { viewModel.navigateToEventList() },
                    onDesignatePayer = { /* Not applicable for viewing events */ },
                    onClearPayer = { /* Not applicable for viewing events */ },
                    onSaveEvent = null, // No save for existing events
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                // Loading state while event is being loaded
                LoadingScreen(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

private const val TAG = "MainActivity"
