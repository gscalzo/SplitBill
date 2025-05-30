package uk.co.effectivecode.firebender.splitbill.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.effectivecode.firebender.splitbill.data.*
import uk.co.effectivecode.firebender.splitbill.service.ReceiptParsingService

sealed class MainScreenState {
    object EventList : MainScreenState()
    object CreateNewBill : MainScreenState()
    data class ViewEvent(val eventId: String) : MainScreenState()
}

sealed class ReceiptUiState {
    object Initial : ReceiptUiState()
    object Loading : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
    data class Success(val editableReceipt: EditableReceipt) : ReceiptUiState()
    data class SplittingMode(val receiptWithSplitting: EditableReceiptWithSplitting) : ReceiptUiState()
    data class BalanceSummary(val receiptWithSplitting: EditableReceiptWithSplitting) : ReceiptUiState()
}

class ReceiptViewModel(
    private val receiptParsingService: ReceiptParsingService,
    private val eventRepository: EventRepository
) : ViewModel() {
    
    private val _mainScreenState = MutableStateFlow<MainScreenState>(MainScreenState.EventList)
    val mainScreenState: StateFlow<MainScreenState> = _mainScreenState.asStateFlow()
    
    private val _receiptUiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Initial)
    val receiptUiState: StateFlow<ReceiptUiState> = _receiptUiState.asStateFlow()
    
    private val _eventList = MutableStateFlow<List<BillEvent>>(emptyList())
    val eventList: StateFlow<List<BillEvent>> = _eventList.asStateFlow()
    
    private val _currentEvent = MutableStateFlow<BillEvent?>(null)
    val currentEvent: StateFlow<BillEvent?> = _currentEvent.asStateFlow()
    
    init {
        Log.d(TAG, "ReceiptViewModel initialized, loading events")
        loadEvents()
    }
    
    private fun loadEvents() {
        Log.d(TAG, "Loading events from repository")
        viewModelScope.launch {
            eventRepository.getAllEvents()
                .onSuccess { events ->
                    Log.d(TAG, "Successfully loaded ${events.size} events from repository")
                    _eventList.value = events
                    events.forEach { event ->
                        Log.d(TAG, "Event in list: ${event.id}, name: ${event.name}")
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load events", exception)
                }
        }
    }
    
    fun navigateToCreateNewBill() {
        Log.d(TAG, "Navigating to create new bill")
        _receiptUiState.value = ReceiptUiState.Initial // Reset receipt state
        _mainScreenState.value = MainScreenState.CreateNewBill
    }
    
    fun navigateToEventList() {
        Log.d(TAG, "Navigating to event list")
        _receiptUiState.value = ReceiptUiState.Initial // Reset receipt state when going back to list
        _mainScreenState.value = MainScreenState.EventList
        loadEvents() // Refresh list
    }
    
    fun navigateToViewEvent(eventId: String) {
        Log.d(TAG, "Navigating to view event: $eventId")
        viewModelScope.launch {
            eventRepository.getEventById(eventId)
                .onSuccess { event ->
                    event?.let {
                        Log.d(TAG, "Successfully loaded event for viewing: ${it.id}, name: ${it.name}")
                        _currentEvent.value = it
                        _receiptUiState.value = ReceiptUiState.BalanceSummary(it.receiptWithSplitting)
                        _mainScreenState.value = MainScreenState.ViewEvent(eventId)
                    } ?: run {
                        Log.w(TAG, "Event not found for ID: $eventId")
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load event for viewing: $eventId", exception)
                }
        }
    }
    
    fun parseReceipt(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _receiptUiState.value = ReceiptUiState.Loading
            receiptParsingService.parseReceipt(context, imageUri)
                .onSuccess { result ->
                    val editableReceipt = EditableReceipt.fromParseResult(result)
                    _receiptUiState.value = ReceiptUiState.Success(editableReceipt)
                }
                .onFailure { exception ->
                    _receiptUiState.value = ReceiptUiState.Error(exception.message ?: "Failed to parse receipt")
                }
        }
    }
    
    // Basic receipt editing functions
    fun addItem(item: ReceiptItem) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.addItem(item)
            _receiptUiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateItem(index: Int, item: ReceiptItem) {
        val currentState = _receiptUiState.value
        when (currentState) {
            is ReceiptUiState.Success -> {
                val updatedReceipt = currentState.editableReceipt.updateItem(index, item)
                _receiptUiState.value = ReceiptUiState.Success(updatedReceipt)
            }
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.updateReceiptItem(index, item)
                _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            else -> { /* No action for other states */ }
        }
    }
    
    fun deleteItem(index: Int) {
        val currentState = _receiptUiState.value
        when (currentState) {
            is ReceiptUiState.Success -> {
                val updatedReceipt = currentState.editableReceipt.deleteItem(index)
                _receiptUiState.value = ReceiptUiState.Success(updatedReceipt)
            }
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.deleteReceiptItem(index)
                _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            else -> { /* No action */ }
        }
    }
    
    fun updateServiceCharge(serviceCharge: Double) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.updateServiceCharge(serviceCharge)
            _receiptUiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    fun updateTotal(total: Double) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.Success) {
            val updatedReceipt = currentState.editableReceipt.updateTotal(total)
            _receiptUiState.value = ReceiptUiState.Success(updatedReceipt)
        }
    }
    
    // Bill splitting functions
    fun enterSplittingMode() {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.Success) {
            val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(currentState.editableReceipt)
            _receiptUiState.value = ReceiptUiState.SplittingMode(receiptWithSplitting)
        }
    }
    
    fun exitSplittingMode() {
        val currentState = _receiptUiState.value
        when (currentState) {
            is ReceiptUiState.SplittingMode -> {
                _receiptUiState.value = ReceiptUiState.Success(currentState.receiptWithSplitting.editableReceipt)
            }
            is ReceiptUiState.BalanceSummary -> {
                 // If exiting from summary of a new bill, go back to editable receipt.
                 // If exiting from summary of a saved event, go back to event list.
                if (_mainScreenState.value is MainScreenState.ViewEvent) {
                     navigateToEventList()
                } else {
                    _receiptUiState.value = ReceiptUiState.Success(currentState.receiptWithSplitting.editableReceipt)
                }
            }
            else -> { /* No action */ }
        }
    }
    
    fun addParticipant(name: String) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.addParticipant(name)
            _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun removeParticipant(participantId: String) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.removeParticipant(participantId)
            _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun assignItemToParticipant(itemIndex: Int, participantId: String) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.assignItemToParticipant(itemIndex, participantId)
            _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun assignItemToEqualSplit(itemIndex: Int, participantIds: List<String>) {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            val updatedReceiptWithSplitting = currentState.receiptWithSplitting.assignItemToEqualSplit(itemIndex, participantIds)
            _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
        }
    }
    
    fun showBalanceSummary() {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.SplittingMode) {
            _receiptUiState.value = ReceiptUiState.BalanceSummary(currentState.receiptWithSplitting)
        }
    }
    
    fun backToSplitting() {
        val currentState = _receiptUiState.value
        if (currentState is ReceiptUiState.BalanceSummary) {
            _receiptUiState.value = ReceiptUiState.SplittingMode(currentState.receiptWithSplitting)
        }
    }
    
    fun designatePayer(payerId: String) {
        val currentState = _receiptUiState.value
        when (currentState) {
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.designatePayer(payerId)
                _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            is ReceiptUiState.BalanceSummary -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.designatePayer(payerId)
                _receiptUiState.value = ReceiptUiState.BalanceSummary(updatedReceiptWithSplitting)
            }
            else -> { /* No action for other states */ }
        }
    }
    
    fun clearPayer() {
        val currentState = _receiptUiState.value
        when (currentState) {
            is ReceiptUiState.SplittingMode -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.clearPayer()
                _receiptUiState.value = ReceiptUiState.SplittingMode(updatedReceiptWithSplitting)
            }
            is ReceiptUiState.BalanceSummary -> {
                val updatedReceiptWithSplitting = currentState.receiptWithSplitting.clearPayer()
                _receiptUiState.value = ReceiptUiState.BalanceSummary(updatedReceiptWithSplitting)
            }
            else -> { /* No action for other states */ }
        }
    }
    
    fun saveCurrentBillAsEvent(eventName: String) {
        val currentState = _receiptUiState.value
        Log.d(TAG, "Attempting to save current bill as event: $eventName, current state: ${currentState::class.simpleName}")
        
        if (currentState is ReceiptUiState.BalanceSummary) {
            viewModelScope.launch {
                val event = BillEvent(
                    name = eventName,
                    receiptWithSplitting = currentState.receiptWithSplitting
                )
                Log.d(TAG, "Created event object: ${event.id}, name: ${event.name}, participants: ${event.receiptWithSplitting.participants.size}")
                
                eventRepository.saveEvent(event)
                    .onSuccess {
                        Log.d(TAG, "Event saved successfully, navigating to event list")
                        navigateToEventList()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to save event: $eventName", exception)
                    }
            }
        } else {
            Log.w(TAG, "Cannot save event - not in BalanceSummary state. Current state: ${currentState::class.simpleName}")
        }
    }
    
    fun updateEventName(eventId: String, newEventName: String) {
        Log.d(TAG, "Updating event name: $eventId to $newEventName")
        viewModelScope.launch {
            eventRepository.updateEventName(eventId, newEventName)
                .onSuccess {
                    Log.d(TAG, "Event name updated successfully")
                    // If it's the current event being viewed, refresh it
                    if (_currentEvent.value?.id == eventId) {
                        navigateToViewEvent(eventId)
                    }
                    // Always refresh the event list
                    loadEvents()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to update event name", exception)
                }
        }
    }
    
    fun updateCurrentEventName(newEventName: String) {
        val event = _currentEvent.value
        val mainState = _mainScreenState.value
        Log.d(TAG, "Updating current event name to: $newEventName")
        
        if (event != null && mainState is MainScreenState.ViewEvent) {
            viewModelScope.launch {
                eventRepository.updateEventName(event.id, newEventName)
                    .onSuccess {
                        Log.d(TAG, "Current event name updated successfully")
                        // Refresh the current event details
                        navigateToViewEvent(event.id)
                        // Also refresh the list in case it's visible
                        loadEvents()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to update current event name", exception)
                    }
            }
        } else {
            Log.w(TAG, "Cannot update current event name - no current event or not in ViewEvent state")
        }
    }
    
    fun deleteEvent(eventId: String) {
        Log.d(TAG, "Deleting event: $eventId")
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
                .onSuccess {
                    Log.d(TAG, "Event deleted successfully")
                    // If current event was deleted, go back to list
                    if (_currentEvent.value?.id == eventId) {
                        navigateToEventList()
                    } else {
                        loadEvents() // Just refresh list
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to delete event", exception)
                }
        }
    }
    
    fun retry() {
        Log.d(TAG, "UI retry requested")
        _receiptUiState.value = ReceiptUiState.Initial
    }
    
    companion object {
        private const val TAG = "ReceiptViewModel"
    }
}
