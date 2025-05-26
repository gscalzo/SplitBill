package uk.co.effectivecode.firebender.splitbill

import org.junit.Test
import org.junit.Assert.*
import uk.co.effectivecode.firebender.splitbill.data.ReceiptCalculation
import uk.co.effectivecode.firebender.splitbill.data.ReceiptItem

class ReceiptCalculationTest {

    @Test
    fun `calculate with single item and no service charge`() {
        // Given
        val items = listOf(ReceiptItem("Pizza", 1, 12.50))
        val serviceCharge = 0.0
        val actualTotal = 12.50

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(12.50, result.subtotal, 0.01)
        assertEquals(0.0, result.serviceCharge, 0.01)
        assertEquals(12.50, result.expectedTotal, 0.01)
        assertEquals(12.50, result.actualTotal, 0.01)
        assertFalse(result.hasDiscrepancy)
        assertEquals(0.0, result.discrepancyAmount, 0.01)
    }

    @Test
    fun `calculate with multiple items and service charge`() {
        // Given
        val items = listOf(
            ReceiptItem("Pizza", 2, 25.00),
            ReceiptItem("Salad", 1, 8.50),
            ReceiptItem("Drink", 3, 9.00)
        )
        val serviceCharge = 4.25
        val actualTotal = 46.75

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(42.50, result.subtotal, 0.01)
        assertEquals(4.25, result.serviceCharge, 0.01)
        assertEquals(46.75, result.expectedTotal, 0.01)
        assertEquals(46.75, result.actualTotal, 0.01)
        assertFalse(result.hasDiscrepancy)
        assertEquals(0.0, result.discrepancyAmount, 0.01)
    }

    @Test
    fun `calculate with discrepancy - actual total higher`() {
        // Given
        val items = listOf(ReceiptItem("Pizza", 1, 15.00))
        val serviceCharge = 1.50
        val actualTotal = 17.00 // Expected: 16.50

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(15.00, result.subtotal, 0.01)
        assertEquals(1.50, result.serviceCharge, 0.01)
        assertEquals(16.50, result.expectedTotal, 0.01)
        assertEquals(17.00, result.actualTotal, 0.01)
        assertTrue(result.hasDiscrepancy)
        assertEquals(0.50, result.discrepancyAmount, 0.01)
    }

    @Test
    fun `calculate with discrepancy - actual total lower`() {
        // Given
        val items = listOf(ReceiptItem("Pizza", 1, 15.00))
        val serviceCharge = 1.50
        val actualTotal = 16.00 // Expected: 16.50

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(15.00, result.subtotal, 0.01)
        assertEquals(1.50, result.serviceCharge, 0.01)
        assertEquals(16.50, result.expectedTotal, 0.01)
        assertEquals(16.00, result.actualTotal, 0.01)
        assertTrue(result.hasDiscrepancy)
        assertEquals(0.50, result.discrepancyAmount, 0.01)
    }

    @Test
    fun `calculate with tiny discrepancy under threshold`() {
        // Given
        val items = listOf(ReceiptItem("Pizza", 1, 15.00))
        val serviceCharge = 1.50
        val actualTotal = 16.505 // Difference of 0.005, under 0.01 threshold

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertFalse("Discrepancy under 0.01 should not be reported", result.hasDiscrepancy)
        assertEquals(0.005, result.discrepancyAmount, 0.001)
    }

    @Test
    fun `calculate with empty items list`() {
        // Given
        val items = emptyList<ReceiptItem>()
        val serviceCharge = 2.00
        val actualTotal = 2.00

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(0.0, result.subtotal, 0.01)
        assertEquals(2.00, result.serviceCharge, 0.01)
        assertEquals(2.00, result.expectedTotal, 0.01)
        assertEquals(2.00, result.actualTotal, 0.01)
        assertFalse(result.hasDiscrepancy)
    }

    @Test
    fun `calculate with zero values`() {
        // Given
        val items = listOf(ReceiptItem("Free Sample", 1, 0.0))
        val serviceCharge = 0.0
        val actualTotal = 0.0

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(0.0, result.subtotal, 0.01)
        assertEquals(0.0, result.serviceCharge, 0.01)
        assertEquals(0.0, result.expectedTotal, 0.01)
        assertEquals(0.0, result.actualTotal, 0.01)
        assertFalse(result.hasDiscrepancy)
    }

    @Test
    fun `calculate with complex quantities and costs`() {
        // Given
        val items = listOf(
            ReceiptItem("Pizza Slice", 3, 18.75), // 3 x 6.25 each
            ReceiptItem("Soft Drink", 2, 5.98),   // 2 x 2.99 each
            ReceiptItem("Side Salad", 1, 4.25)
        )
        val serviceCharge = 2.90
        val actualTotal = 31.88

        // When
        val result = ReceiptCalculation.calculate(items, serviceCharge, actualTotal)

        // Then
        assertEquals(28.98, result.subtotal, 0.01)
        assertEquals(2.90, result.serviceCharge, 0.01)
        assertEquals(31.88, result.expectedTotal, 0.01)
        assertEquals(31.88, result.actualTotal, 0.01)
        assertFalse(result.hasDiscrepancy)
    }
}