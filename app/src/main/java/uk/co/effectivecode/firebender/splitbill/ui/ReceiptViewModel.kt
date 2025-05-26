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
import uk.co.effectivecode.firebender.splitbill.data.EditableReceiptWithSplitting
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem
import uk.co.effectivecode.firebender.splitbill.data.ReceiptParseResult
import uk.co.effectivecode.firebender.splitbill.service.ReceiptParsingService

sealed class ReceiptUiState {
    object Initial : ReceiptUiState()
    object Loading : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
    data class Success(val editableReceipt: EditableReceipt) : ReceiptUiState()
    data class SplittingMode(val receiptWithSplitting: EditableReceiptWithSplitting) : ReceiptUiState()
    data class BalanceSummary(val receiptWithSplitting: EditableReceiptWithSplitting) : ReceiptUiState()
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
    
    // Basic receipt editing functions
    fun addItem(item: ReceiptItem) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.addItem(item)
            _uiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateItem(index: Int, item: ReceiptItem) {
        val currentState = _uiState.value
        when (currentState) {
            is ReceiptUiState.Success -> {
                val updatedReceipt = currentState.editableReceipt.updateItem(index, item)
                _uiState.value = ReceiptUiState.Success(updatedReceipt)
            }
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.updateReceiptItem(index, item)
                _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            else -> {
                // No action for other states
            }
        }
    }
    
    fun deleteItem(index: Int) {
        val currentState = _uiState.value
        when (currentState) {
            is ReceiptUiState.Success -> {
                val updatedReceipt = currentState.editableReceipt.deleteItem(index)
                _uiState.value = ReceiptUiState.Success(updatedReceipt)
            }
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.deleteReceiptItem(index)
                _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            else -> {
                // No action for other states
            }
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
    
    // Bill splitting functions
    fun enterSplittingMode() {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.Success) {
            val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(currentState.editableReceipt)
            _uiState.value = ReceiptUiState.SplittingMode(receiptWithSplitting)
        }
    }
    
    fun exitSplittingMode() {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            _uiState.value = ReceiptUiState.Success(currentState.receiptWithSplitting.editableReceipt)
        } else if (currentState is ReceiptUiState.BalanceSummary) {
            _uiState.value = ReceiptUiState.Success(currentState.receiptWithSplitting.editableReceipt)
        }
    }
    
    fun addParticipant(name: String) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.addParticipant(name)
            _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun removeParticipant(participantId: String) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.removeParticipant(participantId)
            _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun assignItemToParticipant(itemIndex: Int, participantId: String) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.assignItemToParticipant(itemIndex, participantId)
            _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun assignItemToEqualSplit(itemIndex: Int, participantIds: List<String>) {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.assignItemToEqualSplit(itemIndex, participantIds)
            _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun showBalanceSummary() {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            _uiState.value = ReceiptUiState.BalanceSummary(currentState.receiptWithSplitting)
        }
    }
    
    fun backToSplitting() {
        val currentState = _uiState.value
        if (currentState is ReceiptUiState.BalanceSummary) {
            _uiState.value = ReceiptUiState.SplittingMode(currentState.receiptWithSplitting)
        }
    }
    
    fun designatePayer(payerId: String) {
        val currentState = _uiState.value
        when (currentState) {
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.designatePayer(payerId)
                _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            is ReceiptUiState.BalanceSummary -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.designatePayer(payerId)
                _uiState.value = ReceiptUiState.BalanceSummary(updatedReceiptWithSplitting)
            }
            else -> {
                // No action for other states
            }
        }
    }
    
    fun clearPayer() {
        val currentState = _uiState.value
        when (currentState) {
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.clearPayer()
                _uiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            is ReceiptUiState.BalanceSummary -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.clearPayer()
                _uiState.value = ReceiptUiState.BalanceSummary(updatedReceiptWithSplitting)
            }
            else -> {
                // No action for other states
            }
        }
    }
    
    fun retry() {
        _uiState.value = ReceiptUiState.Initial
    }
}
