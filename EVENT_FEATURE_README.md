# Event Saving Feature Implementation

## Overview

The Split-Bill Android app now includes a comprehensive event management system that allows users to
save, view, edit, and delete bill splitting events. This document outlines the implementation
details and user flow.

## ✅ Features Implemented

### 1. **Event Management**

- **Save Events**: After completing bill splitting, users can save events with custom names
- **View Events**: Click on saved events to view their final summary and payment details
- **Edit Events**: Rename existing events through an inline dialog
- **Delete Events**: Remove events with confirmation dialog
- **Event List**: Main screen displays all saved events with key information

### 2. **Data Persistence**

- **Local Storage**: Events saved as JSON files in app's internal storage
- **File-based Repository**: Decoupled architecture allowing future database integration
- **Event Serialization**: Complete event data preserved including participants and assignments

### 3. **User Interface**

- **Material Design 3**: Consistent theming and components throughout
- **Event Cards**: Rich display showing event name, date, total amount, and participant count
- **Floating Action Button**: Easy access to create new bills
- **Navigation**: Seamless transitions between event list, bill creation, and event viewing

## 📱 User Flow

### Creating and Saving a New Event

1. **Launch App** → Event list screen (or welcome screen if no events)
2. **Tap FAB (+)** → Choose photo source (camera/gallery)
3. **Process Receipt** → AI parsing and item extraction
4. **Edit Receipt** → Modify items, service charge, and total
5. **Split Bill** → Add participants and assign items
6. **View Summary** → Review balance summary and designate payer
7. **Save Event** → Enter custom name and save
8. **Return to List** → Event appears in the main list

### Viewing Existing Events

1. **Event List** → Tap on any saved event
2. **View Summary** → See final balance summary with payment details
3. **Exit** → Return to event list

### Managing Events

- **Edit Name**: Tap edit icon on event card → Enter new name → Save
- **Delete Event**: Tap delete icon → Confirm deletion
- **Create New**: Tap FAB from event list

## 🏗️ Architecture

### Clean MVVM Architecture

```
UI Layer (Jetpack Compose)
    ↓
ViewModel (State Management)
    ↓
Use Cases (Business Logic)
    ↓
Repository Interface (Data Abstraction)
    ↓
Data Sources (Local File Storage)
```

### Key Components

#### **Data Layer**

- `BillEvent`: Core data model containing all event information
- `EventRepository`: Interface for data operations (save, load, update, delete)
- `FileEventRepository`: Concrete implementation using local JSON files
- JSON serialization with Gson for data persistence

#### **Domain Layer**

- `EditableReceiptWithSplitting`: Complete receipt with participant assignments
- `BillSplitSummary`: Calculated balances and payment information
- `PaymentSummary`: Final payment breakdown when payer is designated

#### **UI Layer**

- `EventListScreen`: Main screen displaying saved events
- `SaveEventDialog`: Dialog for naming new events
- `EditEventNameDialog`: Dialog for renaming existing events
- `BalanceSummaryScreen`: Event summary display (enhanced for viewing saved events)

#### **State Management**

- `MainScreenState`: Navigation between event list, new bill, and event viewing
- `ReceiptUiState`: Receipt processing and splitting states
- StateFlow for reactive UI updates

## 🔧 Technical Implementation

### File Storage Structure

```
/data/data/[package]/files/events/
├── uuid1.json (Event 1)
├── uuid2.json (Event 2)
└── uuid3.json (Event 3)
```

### Event Data Structure

```json
{
  "id": "uuid-string",
  "name": "Custom Event Name",
  "timestamp": 1234567890000,
  "receiptWithSplitting": {
    "editableReceipt": { ... },
    "participants": [ ... ],
    "assignedItems": [ ... ],
    "billSplitSummary": { ... }
  }
}
```

### Key Classes and Methods

#### **ReceiptViewModel**

```kotlin
// Navigation
fun navigateToEventList()
fun navigateToCreateNewBill()
fun navigateToViewEvent(eventId: String)

// Event Management
fun saveCurrentBillAsEvent(eventName: String)
fun updateEventName(eventId: String, newName: String)
fun deleteEvent(eventId: String)
```

#### **FileEventRepository**

```kotlin
suspend fun saveEvent(event: BillEvent): Result<Unit>
suspend fun getAllEvents(): Result<List<BillEvent>>
suspend fun getEventById(id: String): Result<BillEvent?>
suspend fun updateEventName(id: String, newName: String): Result<Unit>
suspend fun deleteEvent(id: String): Result<Unit>
```

## 🧪 Testing

### Unit Tests Coverage

- **Event Creation**: Verify proper event instantiation and data integrity
- **Data Validation**: Test event ID uniqueness and timestamp generation
- **Name Modification**: Ensure event names can be updated
- **Data Preservation**: Confirm participant and item data is maintained

### Test Files

- `FileEventRepositoryTest.kt`: Core event functionality tests
- `EditableReceiptTest.kt`: Receipt calculation and modification tests
- `BillSplittingTest.kt`: Bill splitting logic tests

## 🔮 Future Extensibility

The architecture supports easy extension for:

### **Remote Storage**

- Replace `FileEventRepository` with `RemoteEventRepository`
- Add cloud synchronization capabilities
- Implement offline-first architecture

### **Enhanced Features**

- Event sharing between users
- Export functionality (PDF, CSV)
- Event search and filtering
- Category tagging
- Recurring bill templates

### **Database Integration**

- Room database for local storage
- SQLite queries for advanced filtering
- Migration from file-based storage

## 📝 String Resources

All user-facing text is externalized to `strings.xml` for:

- Internationalization support
- Consistent messaging
- Easy maintenance

Key string resources include:

- Event management actions
- Dialog titles and messages
- Navigation labels
- Error messages

## 🛠️ Development Notes

### **File I/O Best Practices**

- Proper use of `withContext(Dispatchers.IO)` for file operations
- File flushing to ensure data persistence
- Error handling with Result wrapper

### **State Management**

- Proper UI state resets during navigation
- Event list refresh after save/edit/delete operations
- Coroutine-based async operations

### **Material Design Compliance**

- Consistent elevation and theming
- Proper use of Material 3 components
- Accessibility considerations

---

## Summary

The event saving feature provides a complete bill management solution with:

- ✅ Persistent local storage
- ✅ Intuitive user interface
- ✅ Clean architecture
- ✅ Comprehensive testing
- ✅ Future extensibility

Users can now create bills, split them among participants, save them with custom names, and return
to view the final summaries at any time. The decoupled architecture ensures the system can easily
evolve to support remote storage and additional features in the future.