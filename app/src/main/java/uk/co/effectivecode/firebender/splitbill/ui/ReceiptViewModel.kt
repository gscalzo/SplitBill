package uk.co.effectivecode.firebender.splitbill.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.effectivecode.firebender.splitbill.data.EditableReceipt
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem
import uk.co.effectivecode.firebender.splitbill.data.ReceiptParseResult
import uk.co.effectivecode.firebender.splitbill.service.ReceiptParsingService

sealed class ReceiptUiState {
    object Initial : ReceiptUiState()
    object Loading : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
    data class Success(val editableReceipt: EditableReceipt) : ReceiptUiState()
}

class ReceiptViewModel(
    private val receiptParsingService: ReceiptParsingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Initial)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()
    
    fun parseReceipt(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Loading
            
            receiptParsingService.parseReceipt(context, imageUri)
                .onSuccess { result ->
                    val editableReceipt = EditableReceipt.fromParseResult(result)
                    _uiState.value = ReceiptUiState.Success(editableReceipt)
                }
                .onFailure { exception ->
                    _uiState.value = ReceiptUiState.Error(
                        exception.message ?: "Failed to parse receipt"
                    )
                }
        }
    }
    
    fun addItem(item: ReceiptItem) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.addItem(item)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateItem(index: Int, item: ReceiptItem) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.updateItem(index, item)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun deleteItem(index: Int) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.deleteItem(index)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateServiceCharge(serviceCharge: Double) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.updateServiceCharge(serviceCharge)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateTotal(total: Double) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.updateTotal(total)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun retry() {
        _uiState.value = ReceiptUiState.Initial
    }
}