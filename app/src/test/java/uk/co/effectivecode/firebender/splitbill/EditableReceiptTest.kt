package uk.co.effectivecode.firebender.splitbill

import org.junit.Test
import org.junit.Assert.*
import uk.co.effectivecode.firebender.splitbill.data.EditableReceipt
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem
import uk.co.effectivecode.firebender.splitbill.data.ReceiptParseResult

class EditableReceiptTest {

    private fun createSampleParseResult(): ReceiptParseResult {
        return ReceiptParseResult(
            error = null,
            items = listOf(
                ReceiptItem("Pizza", 2, 20.00),
                ReceiptItem("Salad", 1, 8.50)
            ),
            service = 2.85,
            total = 31.35
        )
    }

    @Test
    fun `fromParseResult creates EditableReceipt correctly`() {
        // Given
        val parseResult = createSampleParseResult()

        // When
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)

        // Then
        assertEquals(parseResult, editableReceipt.originalResult)
        assertEquals(2, editableReceipt.items.size)
        assertEquals("Pizza", editableReceipt.items[0].name)
        assertEquals("Salad", editableReceipt.items[1].name)
        assertEquals(2.85, editableReceipt.serviceCharge, 0.01)
        assertEquals(31.35, editableReceipt.total, 0.01)
        
        // Check calculation
        assertEquals(28.50, editableReceipt.calculation.subtotal, 0.01)
        assertEquals(31.35, editableReceipt.calculation.expectedTotal, 0.01)
        assertFalse(editableReceipt.calculation.hasDiscrepancy)
    }

    @Test
    fun `fromParseResult handles null values`() {
        // Given
        val parseResult = ReceiptParseResult(
            error = "Parse error",
            items = null,
            service = null,
            total = null
        )

        // When
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)

        // Then
        assertTrue(editableReceipt.items.isEmpty())
        assertEquals(0.0, editableReceipt.serviceCharge, 0.01)
        assertEquals(0.0, editableReceipt.total, 0.01)
        assertEquals(0.0, editableReceipt.calculation.subtotal, 0.01)
    }

    @Test
    fun `updateItems recalculates totals correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val newItems = listOf(
            ReceiptItem("Burger", 1, 12.00),
            ReceiptItem("Fries", 2, 8.00)
        )

        // When
        val updated = editableReceipt.updateItems(newItems)

        // Then
        assertEquals(newItems, updated.items)
        assertEquals(20.00, updated.calculation.subtotal, 0.01)
        assertEquals(22.85, updated.calculation.expectedTotal, 0.01) // 20.00 + 2.85 service
        assertTrue(updated.calculation.hasDiscrepancy) // Original total was 31.35
    }

    @Test
    fun `updateServiceCharge recalculates totals correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val newServiceCharge = 5.00

        // When
        val updated = editableReceipt.updateServiceCharge(newServiceCharge)

        // Then
        assertEquals(5.00, updated.serviceCharge, 0.01)
        assertEquals(28.50, updated.calculation.subtotal, 0.01) // Items unchanged
        assertEquals(33.50, updated.calculation.expectedTotal, 0.01) // 28.50 + 5.00
        assertTrue(updated.calculation.hasDiscrepancy) // Original total was 31.35
    }

    @Test
    fun `updateTotal recalculates discrepancy correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val newTotal = 30.00

        // When
        val updated = editableReceipt.updateTotal(newTotal)

        // Then
        assertEquals(30.00, updated.total, 0.01)
        assertEquals(28.50, updated.calculation.subtotal, 0.01) // Items unchanged
        assertEquals(31.35, updated.calculation.expectedTotal, 0.01) // 28.50 + 2.85
        assertTrue(updated.calculation.hasDiscrepancy) // Expected 31.35, actual 30.00
        assertEquals(1.35, updated.calculation.discrepancyAmount, 0.01)
    }

    @Test
    fun `addItem appends new item and recalculates`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val newItem = ReceiptItem("Dessert", 1, 6.50)

        // When
        val updated = editableReceipt.addItem(newItem)

        // Then
        assertEquals(3, updated.items.size)
        assertEquals("Dessert", updated.items[2].name)
        assertEquals(35.00, updated.calculation.subtotal, 0.01) // 28.50 + 6.50
        assertEquals(37.85, updated.calculation.expectedTotal, 0.01) // 35.00 + 2.85
        assertTrue(updated.calculation.hasDiscrepancy) // Original total was 31.35
    }

    @Test
    fun `updateItem at valid index updates correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val updatedItem = ReceiptItem("Large Pizza", 2, 25.00)

        // When
        val updated = editableReceipt.updateItem(0, updatedItem)

        // Then
        assertEquals(2, updated.items.size)
        assertEquals("Large Pizza", updated.items[0].name)
        assertEquals(25.00, updated.items[0].cost, 0.01)
        assertEquals("Salad", updated.items[1].name) // Second item unchanged
        assertEquals(33.50, updated.calculation.subtotal, 0.01) // 25.00 + 8.50
        assertEquals(36.35, updated.calculation.expectedTotal, 0.01) // 33.50 + 2.85
    }

    @Test
    fun `updateItem at invalid index returns unchanged receipt`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())
        val updatedItem = ReceiptItem("New Item", 1, 10.00)

        // When
        val updated1 = editableReceipt.updateItem(-1, updatedItem)
        val updated2 = editableReceipt.updateItem(5, updatedItem)

        // Then
        assertEquals(editableReceipt, updated1)
        assertEquals(editableReceipt, updated2)
    }

    @Test
    fun `deleteItem at valid index removes item and recalculates`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())

        // When
        val updated = editableReceipt.deleteItem(0) // Remove Pizza

        // Then
        assertEquals(1, updated.items.size)
        assertEquals("Salad", updated.items[0].name)
        assertEquals(8.50, updated.calculation.subtotal, 0.01)
        assertEquals(11.35, updated.calculation.expectedTotal, 0.01) // 8.50 + 2.85
        assertTrue(updated.calculation.hasDiscrepancy) // Original total was 31.35
    }

    @Test
    fun `deleteItem at invalid index returns unchanged receipt`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())

        // When
        val updated1 = editableReceipt.deleteItem(-1)
        val updated2 = editableReceipt.deleteItem(5)

        // Then
        assertEquals(editableReceipt, updated1)
        assertEquals(editableReceipt, updated2)
    }

    @Test
    fun `deleteItem removes last item correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())

        // When
        val updated = editableReceipt.deleteItem(1) // Remove Salad

        // Then
        assertEquals(1, updated.items.size)
        assertEquals("Pizza", updated.items[0].name)
        assertEquals(20.00, updated.calculation.subtotal, 0.01)
        assertEquals(22.85, updated.calculation.expectedTotal, 0.01) // 20.00 + 2.85
    }

    @Test
    fun `multiple operations chain correctly`() {
        // Given
        val editableReceipt = EditableReceipt.fromParseResult(createSampleParseResult())

        // When
        val updated = editableReceipt
            .addItem(ReceiptItem("Drink", 1, 3.00))
            .updateServiceCharge(4.00)
            .updateTotal(36.00)
            .deleteItem(1) // Remove Salad

        // Then
        assertEquals(2, updated.items.size)
        assertEquals("Pizza", updated.items[0].name)
        assertEquals("Drink", updated.items[1].name)
        assertEquals(4.00, updated.serviceCharge, 0.01)
        assertEquals(36.00, updated.total, 0.01)
        assertEquals(23.00, updated.calculation.subtotal, 0.01) // Pizza + Drink
        assertEquals(27.00, updated.calculation.expectedTotal, 0.01) // 23.00 + 4.00
        assertTrue(updated.calculation.hasDiscrepancy) // Expected 27.00, actual 36.00
    }
}