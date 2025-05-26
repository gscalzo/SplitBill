package uk.co.effectivecode.firebender.splitbill

import org.junit.Test
import org.junit.Assert.*
import uk.co.effectivecode.firebender.splitbill.data.*

class BillSplittingTest {

    private fun createSampleReceipt(): EditableReceipt {
        val parseResult = ReceiptParseResult(
            items = listOf(
                ReceiptItem("Pizza", 2, 20.00),
                ReceiptItem("Salad", 1, 8.00),
                ReceiptItem("Drinks", 3, 9.00)
            ),
            service = 3.70,
            total = 40.70
        )
        return EditableReceipt.fromParseResult(parseResult)
    }

    @Test
    fun `Participant creation generates unique IDs`() {
        // Given & When
        val participant1 = Participant.create("Alice")
        val participant2 = Participant.create("Bob")
        
        // Then
        assertNotEquals(participant1.id, participant2.id)
        assertEquals("Alice", participant1.name)
        assertEquals("Bob", participant2.name)
    }

    @Test
    fun `EditableReceiptWithSplitting created from EditableReceipt`() {
        // Given
        val editableReceipt = createSampleReceipt()
        
        // When
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(editableReceipt)
        
        // Then
        assertEquals(editableReceipt, receiptWithSplitting.editableReceipt)
        assertTrue(receiptWithSplitting.participants.isEmpty())
        assertEquals(3, receiptWithSplitting.assignedItems.size)
        receiptWithSplitting.assignedItems.forEach { assignedItem ->
            assertTrue(assignedItem.assignment is ItemAssignment.Unassigned)
            assertFalse(assignedItem.isAssigned)
        }
    }

    @Test
    fun `addParticipant adds participant and recalculates`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
        
        // When
        val updated = receiptWithSplitting.addParticipant("Alice")
        
        // Then
        assertEquals(1, updated.participants.size)
        assertEquals("Alice", updated.participants[0].name)
        assertEquals(1, updated.billSplitSummary.participants.size)
    }

    @Test
    fun `removeParticipant removes participant and unassigns items`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
        
        val aliceId = receiptWithSplitting.participants.find { it.name == "Alice" }!!.id
        val updated1 = receiptWithSplitting.assignItemToParticipant(0, aliceId)
        
        // When
        val updated2 = updated1.removeParticipant(aliceId)
        
        // Then
        assertEquals(1, updated2.participants.size)
        assertEquals("Bob", updated2.participants[0].name)
        assertTrue(updated2.assignedItems[0].assignment is ItemAssignment.Unassigned)
    }

    @Test
    fun `assignItemToParticipant assigns item correctly`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
        
        val aliceId = receiptWithSplitting.participants[0].id
        
        // When
        val updated = receiptWithSplitting.assignItemToParticipant(0, aliceId)
        
        // Then
        val assignment = updated.assignedItems[0].assignment
        assertTrue(assignment is ItemAssignment.IndividualAssignment)
        assertEquals(aliceId, (assignment as ItemAssignment.IndividualAssignment).participantId)
        assertTrue(updated.assignedItems[0].isAssigned)
    }

    @Test
    fun `assignItemToEqualSplit assigns item to multiple participants`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
        
        val participantIds = receiptWithSplitting.participants.map { it.id }
        
        // When
        val updated = receiptWithSplitting.assignItemToEqualSplit(0, participantIds)
        
        // Then
        val assignment = updated.assignedItems[0].assignment
        assertTrue(assignment is ItemAssignment.EqualSplit)
        assertEquals(participantIds.toSet(), (assignment as ItemAssignment.EqualSplit).participantIds.toSet())
        assertTrue(updated.assignedItems[0].isAssigned)
    }

    @Test
    fun `BillSplitSummary calculates individual assignment correctly`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
        
        val aliceId = receiptWithSplitting.participants[0].id
        val bobId = receiptWithSplitting.participants[1].id
        
        // When - Alice gets Pizza (£20), Bob gets Salad (£8), Drinks (£9) unassigned
        val updated = receiptWithSplitting
            .assignItemToParticipant(0, aliceId) // Pizza to Alice
            .assignItemToParticipant(1, bobId)   // Salad to Bob
        
        // Then
        val summary = updated.billSplitSummary
        assertEquals(28.00, summary.totalAssigned, 0.01) // 20 + 8
        assertEquals(9.00, summary.totalUnassigned, 0.01) // Drinks
        assertFalse(summary.isFullyAssigned)
        
        val aliceBalance = summary.balances.find { it.participant.id == aliceId }!!
        assertEquals(20.00, aliceBalance.subtotal, 0.01)
        assertEquals(1.85, aliceBalance.serviceCharge, 0.01) // 3.70 / 2
        assertEquals(21.85, aliceBalance.total, 0.01)
        assertEquals(1, aliceBalance.itemsOwed.size)
        assertEquals("Pizza", aliceBalance.itemsOwed[0].name)
        
        val bobBalance = summary.balances.find { it.participant.id == bobId }!!
        assertEquals(8.00, bobBalance.subtotal, 0.01)
        assertEquals(1.85, bobBalance.serviceCharge, 0.01) // 3.70 / 2
        assertEquals(9.85, bobBalance.total, 0.01)
    }

    @Test
    fun `BillSplitSummary calculates equal split correctly`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
            .addParticipant("Charlie")
        
        val participantIds = receiptWithSplitting.participants.map { it.id }
        
        // When - Pizza split 3 ways, others unassigned
        val updated = receiptWithSplitting.assignItemToEqualSplit(0, participantIds)
        
        // Then
        val summary = updated.billSplitSummary
        assertEquals(20.00, summary.totalAssigned, 0.01) // Pizza
        assertEquals(17.00, summary.totalUnassigned, 0.01) // Salad + Drinks
        
        summary.balances.forEach { balance ->
            assertEquals(6.67, balance.subtotal, 0.01) // 20 / 3
            assertEquals(1.23, balance.serviceCharge, 0.01) // 3.70 / 3
            assertEquals(7.90, balance.total, 0.01)
            assertEquals(1, balance.itemsOwed.size)
            assertTrue(balance.itemsOwed[0].name.contains("split 3 ways"))
        }
    }

    @Test
    fun `BillSplitSummary fully assigned scenario`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
        
        val aliceId = receiptWithSplitting.participants[0].id
        val bobId = receiptWithSplitting.participants[1].id
        
        // When - All items assigned
        val updated = receiptWithSplitting
            .assignItemToParticipant(0, aliceId) // Pizza to Alice
            .assignItemToParticipant(1, bobId)   // Salad to Bob
            .assignItemToParticipant(2, aliceId) // Drinks to Alice
        
        // Then
        val summary = updated.billSplitSummary
        assertEquals(37.00, summary.totalAssigned, 0.01) // All items
        assertEquals(0.00, summary.totalUnassigned, 0.01)
        assertTrue(summary.isFullyAssigned)
        
        val aliceBalance = summary.balances.find { it.participant.id == aliceId }!!
        assertEquals(29.00, aliceBalance.subtotal, 0.01) // Pizza + Drinks
        assertEquals(2, aliceBalance.itemsOwed.size)
        
        val bobBalance = summary.balances.find { it.participant.id == bobId }!!
        assertEquals(8.00, bobBalance.subtotal, 0.01) // Salad only
        assertEquals(1, bobBalance.itemsOwed.size)
    }

    @Test
    fun `updateReceiptItem updates both receipt and assignment`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
        
        val aliceId = receiptWithSplitting.participants[0].id
        val updated1 = receiptWithSplitting.assignItemToParticipant(0, aliceId)
        
        // When
        val newItem = ReceiptItem("Large Pizza", 2, 25.00)
        val updated2 = updated1.updateReceiptItem(0, newItem)
        
        // Then
        assertEquals("Large Pizza", updated2.assignedItems[0].receiptItem.name)
        assertEquals(25.00, updated2.assignedItems[0].receiptItem.cost, 0.01)
        assertTrue(updated2.assignedItems[0].assignment is ItemAssignment.IndividualAssignment)
        
        val aliceBalance = updated2.billSplitSummary.balances[0]
        assertEquals(25.00, aliceBalance.subtotal, 0.01)
    }

    @Test
    fun `addReceiptItem adds item as unassigned`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
        
        // When
        val newItem = ReceiptItem("Dessert", 1, 5.00)
        val updated = receiptWithSplitting.addReceiptItem(newItem)
        
        // Then
        assertEquals(4, updated.assignedItems.size)
        val newAssignedItem = updated.assignedItems.last()
        assertEquals("Dessert", newAssignedItem.receiptItem.name)
        assertTrue(newAssignedItem.assignment is ItemAssignment.Unassigned)
        assertFalse(newAssignedItem.isAssigned)
    }

    @Test
    fun `deleteReceiptItem removes item and updates calculations`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
        
        val aliceId = receiptWithSplitting.participants[0].id
        val updated1 = receiptWithSplitting.assignItemToParticipant(0, aliceId) // Assign Pizza
        
        // When
        val updated2 = updated1.deleteReceiptItem(0) // Delete Pizza
        
        // Then
        assertEquals(2, updated2.assignedItems.size)
        assertEquals("Salad", updated2.assignedItems[0].receiptItem.name)
        
        val aliceBalance = updated2.billSplitSummary.balances[0]
        assertEquals(0.00, aliceBalance.subtotal, 0.01) // No items assigned anymore
    }

    @Test
    fun `complex scenario with mixed assignments`() {
        // Given
        val receiptWithSplitting = EditableReceiptWithSplitting.fromEditableReceipt(createSampleReceipt())
            .addParticipant("Alice")
            .addParticipant("Bob")
            .addParticipant("Charlie")
        
        val alice = receiptWithSplitting.participants[0]
        val bob = receiptWithSplitting.participants[1]
        val charlie = receiptWithSplitting.participants[2]
        
        // When - Mixed assignment scenario
        val updated = receiptWithSplitting
            // Pizza split between Alice and Bob
            .assignItemToEqualSplit(0, listOf(alice.id, bob.id))
            // Salad goes to Charlie
            .assignItemToParticipant(1, charlie.id)
            // Drinks split between all three
            .assignItemToEqualSplit(2, listOf(alice.id, bob.id, charlie.id))
        
        // Then
        val summary = updated.billSplitSummary
        assertTrue(summary.isFullyAssigned)
        assertEquals(37.00, summary.totalAssigned, 0.01)
        
        val aliceBalance = summary.balances.find { it.participant.name == "Alice" }!!
        assertEquals(13.00, aliceBalance.subtotal, 0.01) // (20/2) + (9/3) = 10 + 3
        assertEquals(2, aliceBalance.itemsOwed.size)
        
        val bobBalance = summary.balances.find { it.participant.name == "Bob" }!!
        assertEquals(13.00, bobBalance.subtotal, 0.01) // Same as Alice
        
        val charlieBalance = summary.balances.find { it.participant.name == "Charlie" }!!
        assertEquals(11.00, charlieBalance.subtotal, 0.01) // 8 + (9/3) = 8 + 3
        assertEquals(2, charlieBalance.itemsOwed.size)
        
        // Service charge split equally among 3
        summary.balances.forEach { balance ->
            assertEquals(1.23, balance.serviceCharge, 0.01) // 3.70 / 3
        }
    }

    @Test
    fun `receipt with discrepancy should not allow bill splitting`() {
        // Given - Create a receipt with intentional discrepancy
        val parseResult = ReceiptParseResult(
            items = listOf(
                ReceiptItem("Pizza", 1, 15.00),
                ReceiptItem("Drink", 1, 5.00)
            ),
            service = 2.00,
            total = 25.00 // Expected: 22.00 (15 + 5 + 2), Actual: 25.00 - creates discrepancy
        )
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)
        
        // When & Then
        assertTrue("Receipt should have discrepancy", editableReceipt.calculation.hasDiscrepancy)
        assertEquals(20.00, editableReceipt.calculation.subtotal, 0.01)
        assertEquals(22.00, editableReceipt.calculation.expectedTotal, 0.01)
        assertEquals(25.00, editableReceipt.calculation.actualTotal, 0.01)
        assertEquals(3.00, editableReceipt.calculation.discrepancyAmount, 0.01)
    }

    @Test
    fun `receipt without discrepancy should allow bill splitting`() {
        // Given - Create a receipt without discrepancy
        val parseResult = ReceiptParseResult(
            items = listOf(
                ReceiptItem("Pizza", 1, 15.00),
                ReceiptItem("Drink", 1, 5.00)
            ),
            service = 2.00,
            total = 22.00 // Expected: 22.00 (15 + 5 + 2), Actual: 22.00 - no discrepancy
        )
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)
        
        // When & Then
        assertFalse("Receipt should not have discrepancy", editableReceipt.calculation.hasDiscrepancy)
        assertEquals(20.00, editableReceipt.calculation.subtotal, 0.01)
        assertEquals(22.00, editableReceipt.calculation.expectedTotal, 0.01)
        assertEquals(22.00, editableReceipt.calculation.actualTotal, 0.01)
        assertEquals(0.00, editableReceipt.calculation.discrepancyAmount, 0.01)
    }

    @Test
    fun `fixing discrepancy should enable bill splitting`() {
        // Given - Start with receipt that has discrepancy
        val parseResult = ReceiptParseResult(
            items = listOf(ReceiptItem("Pizza", 1, 15.00)),
            service = 1.50,
            total = 20.00 // Expected: 16.50, creates discrepancy
        )
        val editableReceipt = EditableReceipt.fromParseResult(parseResult)
        
        // When - Fix the total to match expected
        val fixedReceipt = editableReceipt.updateTotal(16.50)
        
        // Then
        assertTrue("Original receipt should have discrepancy", editableReceipt.calculation.hasDiscrepancy)
        assertFalse("Fixed receipt should not have discrepancy", fixedReceipt.calculation.hasDiscrepancy)
        assertEquals(16.50, fixedReceipt.calculation.expectedTotal, 0.01)
        assertEquals(16.50, fixedReceipt.calculation.actualTotal, 0.01)
    }
}
