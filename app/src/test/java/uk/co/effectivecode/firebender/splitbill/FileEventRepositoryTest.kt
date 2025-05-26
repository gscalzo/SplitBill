package uk.co.effectivecode.firebender.splitbill

import org.junit.Test
import org.junit.Assert.*
import uk.co.effectivecode.firebender.splitbill.data.*

class FileEventRepositoryTest {

    private fun createSampleEvent(): BillEvent {
        val items = listOf(
            ReceiptItem("Pizza", 1, 15.99),
            ReceiptItem("Soda", 2, 2.50)
        )
        val parseResult = ReceiptParseResult(
            error = null,
            items = items,
            service = 2.00,
            total = 20.99
        )
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(editableReceipt)
            .addParticipant("Alice")
            .addParticipant("Bob")

        return BillEvent(
            id = "test-event-id",
            name = "Test Event",
            timestamp = 1234567890L,
            receiptWithSplitting = receiptWithSplitting
        )
    }

    @Test
    fun `test create sample event returns valid event`() {
        val event = createSampleEvent()
        
        assertEquals("test-event-id", event.id)
        assertEquals("Test Event", event.name)
        assertEquals(1234567890L, event.timestamp)
        assertEquals(2, event.receiptWithSplitting.participants.size)
        assertEquals("Alice", event.receiptWithSplitting.participants[0].name)
        assertEquals("Bob", event.receiptWithSplitting.participants[1].name)
        assertEquals(20.99, event.receiptWithSplitting.editableReceipt.total, 0.01)
    }

    @Test
    fun `test event contains expected data`() {
        val event = createSampleEvent()
        
        // Test receipt items
        assertEquals(2, event.receiptWithSplitting.editableReceipt.items.size)
        assertEquals("Pizza", event.receiptWithSplitting.editableReceipt.items[0].name)
        assertEquals("Soda", event.receiptWithSplitting.editableReceipt.items[1].name)
        
        // Test participants
        assertEquals(2, event.receiptWithSplitting.participants.size)
        assertTrue(event.receiptWithSplitting.participants.any { it.name == "Alice" })
        assertTrue(event.receiptWithSplitting.participants.any { it.name == "Bob" })
        
        // Test totals
        assertEquals(2.00, event.receiptWithSplitting.editableReceipt.serviceCharge, 0.01)
        assertEquals(20.99, event.receiptWithSplitting.editableReceipt.total, 0.01)
    }
}
