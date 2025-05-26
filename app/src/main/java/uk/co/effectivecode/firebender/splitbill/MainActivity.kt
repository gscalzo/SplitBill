package uk.co.effectivecode.firebender.splitbill

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem
import uk.co.effectivecode.firebender.splitbill.data.ReceiptParseResult
import uk.co.effectivecode.firebender.splitbill.service.OpenAIService
import uk.co.effectivecode.firebender.splitbill.service.ReceiptParsingService
import uk.co.effectivecode.firebender.splitbill.ui.theme.SplitBillTheme
import uk.co.effectivecode.firebender.splitbill.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplitBillTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Create ViewModel with dependency injection
    val receiptService: ReceiptParsingService = remember { OpenAIService(useMock = false) }
    val viewModel: ReceiptViewModel = viewModel { ReceiptViewModel(receiptService) }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Receipt")
            }
        }
    ) { innerPadding ->
        MainScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
        
        // Image source chooser dialog
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Add Receipt") },
                text = { Text("Choose how to add a receipt photo") },
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
                        Text("Take Photo")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Choose from Gallery")
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreen(
    viewModel: ReceiptViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = uiState) {
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
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
