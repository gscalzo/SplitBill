package uk.co.effectivecode.firebender.splitbill.data

import com.google.gson.annotations.SerializedName

data class ReceiptParseResult(
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("items")
    val items: List<ReceiptItem>? = null,
    @SerializedName("service")
    val service: Double? = null,
    @SerializedName("total")
    val total: Double? = null
)

data class ReceiptItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("quantity")
    val quantity: Int = 1,
    @SerializedName("cost")
    val cost: Double
)

// OpenAI API request models
data class OpenAIRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<OpenAIMessage>,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1000
)

data class OpenAIMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: List<MessageContent>
)

data class MessageContent(
    @SerializedName("type")
    val type: String,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url")
    val url: String
)

data class ResponseFormat(
    @SerializedName("type")
    val type: String = "json_schema",
    @SerializedName("json_schema")
    val jsonSchema: JsonSchema
)

data class JsonSchema(
    @SerializedName("name")
    val name: String = "receipt_parse_result",
    @SerializedName("strict")
    val strict: Boolean = true,
    @SerializedName("schema")
    val schema: Schema
)

data class Schema(
    @SerializedName("type")
    val type: String = "object",
    @SerializedName("properties")
    val properties: Map<String, Property>,
    @SerializedName("required")
    val required: List<String> = emptyList(),
    @SerializedName("additionalProperties")
    val additionalProperties: Boolean = false
)

data class Property(
    @SerializedName("type")
    val type: Any, // Can be String or List<String> for union types like ["string", "null"]
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("items")
    val items: Property? = null,
    @SerializedName("properties")
    val properties: Map<String, Property>? = null,
    @SerializedName("required")
    val required: List<String>? = null,
    @SerializedName("additionalProperties")
    val additionalProperties: Boolean? = null
)

// OpenAI API response models
data class OpenAIResponse(
    @SerializedName("choices")
    val choices: List<Choice>
)

data class Choice(
    @SerializedName("message")
    val message: ResponseMessage
)

data class ResponseMessage(
    @SerializedName("content")
    val content: String
)

// Domain models for business logic
data class ReceiptCalculation(
    val subtotal: Double,
    val serviceCharge: Double,
    val expectedTotal: Double,
    val actualTotal: Double,
    val hasDiscrepancy: Boolean,
    val discrepancyAmount: Double
) {
    companion object {
        fun calculate(items: List<ReceiptItem>, serviceCharge: Double, actualTotal: Double): ReceiptCalculation {
            val subtotal = items.sumOf { it.cost }
            val expectedTotal = subtotal + serviceCharge
            val discrepancyAmount = kotlin.math.abs(expectedTotal - actualTotal)
            val hasDiscrepancy = discrepancyAmount > 0.01
            
            return ReceiptCalculation(
                subtotal = subtotal,
                serviceCharge = serviceCharge,
                expectedTotal = expectedTotal,
                actualTotal = actualTotal,
                hasDiscrepancy = hasDiscrepancy,
                discrepancyAmount = discrepancyAmount
            )
        }
    }
}

data class EditableReceipt(
    val originalResult: ReceiptParseResult,
    val items: List<ReceiptItem>,
    val serviceCharge: Double,
    val total: Double,
    val calculation: ReceiptCalculation
) {
    companion object {
        fun fromParseResult(result: ReceiptParseResult): EditableReceipt {
            val items = result.items ?: emptyList()
            val serviceCharge = result.service ?: 0.0
            val total = result.total ?: 0.0
            val calculation = ReceiptCalculation.calculate(items, serviceCharge, total)
            
            return EditableReceipt(
                originalResult = result,
                items = items,
                serviceCharge = serviceCharge,
                total = total,
                calculation = calculation
            )
        }
    }
    
    fun updateItems(newItems: List<ReceiptItem>): EditableReceipt {
        val newCalculation = ReceiptCalculation.calculate(newItems, serviceCharge, total)
        return copy(items = newItems, calculation = newCalculation)
    }
    
    fun updateServiceCharge(newServiceCharge: Double): EditableReceipt {
        val newCalculation = ReceiptCalculation.calculate(items, newServiceCharge, total)
        return copy(serviceCharge = newServiceCharge, calculation = newCalculation)
    }
    
    fun updateTotal(newTotal: Double): EditableReceipt {
        val newCalculation = ReceiptCalculation.calculate(items, serviceCharge, newTotal)
        return copy(total = newTotal, calculation = newCalculation)
    }
    
    fun addItem(item: ReceiptItem): EditableReceipt {
        return updateItems(items + item)
    }
    
    fun updateItem(index: Int, item: ReceiptItem): EditableReceipt {
        if (index < 0 || index >= items.size) return this
        val newItems = items.toMutableList()
        newItems[index] = item
        return updateItems(newItems)
    }
    
    fun deleteItem(index: Int): EditableReceipt {
        if (index < 0 || index >= items.size) return this
        val newItems = items.toMutableList()
        newItems.removeAt(index)
        return updateItems(newItems)
    }
}

// Participant and Bill Splitting Models
data class Participant(
    val id: String,
    val name: String
) {
    companion object {
        fun create(name: String): Participant {
            return Participant(
                id = java.util.UUID.randomUUID().toString(),
                name = name.trim()
            )
        }
    }
}

sealed class ItemAssignment {
    data class EqualSplit(val participantIds: List<String>) : ItemAssignment()
    data class IndividualAssignment(val participantId: String) : ItemAssignment()
    object Unassigned : ItemAssignment()
}

data class AssignedReceiptItem(
    val receiptItem: ReceiptItem,
    val assignment: ItemAssignment
) {
    val isAssigned: Boolean
        get() = assignment !is ItemAssignment.Unassigned
}

data class ParticipantBalance(
    val participant: Participant,
    val itemsOwed: List<ReceiptItem>,
    val subtotal: Double,
    val serviceCharge: Double,
    val total: Double
)

data class BillSplitSummary(
    val participants: List<Participant>,
    val assignedItems: List<AssignedReceiptItem>,
    val balances: List<ParticipantBalance>,
    val totalAssigned: Double,
    val totalUnassigned: Double,
    val isFullyAssigned: Boolean,
    val payerId: String? = null
) {
    val paymentSummary: PaymentSummary?
        get() = payerId?.let { PaymentSummary.calculate(participants, balances, it) }

    companion object {
        fun calculate(
            participants: List<Participant>,
            assignedItems: List<AssignedReceiptItem>,
            serviceCharge: Double,
            payerId: String? = null
        ): BillSplitSummary {
            val balances = participants.map { participant ->
                calculateParticipantBalance(participant, assignedItems, serviceCharge, participants.size)
            }
            
            val totalAssigned = assignedItems.filter { it.isAssigned }.sumOf { it.receiptItem.cost }
            val totalUnassigned = assignedItems.filter { !it.isAssigned }.sumOf { it.receiptItem.cost }
            val isFullyAssigned = totalUnassigned == 0.0
            
            return BillSplitSummary(
                participants = participants,
                assignedItems = assignedItems,
                balances = balances,
                totalAssigned = totalAssigned,
                totalUnassigned = totalUnassigned,
                isFullyAssigned = isFullyAssigned,
                payerId = payerId
            )
        }
        
        private fun calculateParticipantBalance(
            participant: Participant,
            assignedItems: List<AssignedReceiptItem>,
            serviceCharge: Double,
            totalParticipants: Int
        ): ParticipantBalance {
            val itemsOwed = mutableListOf<ReceiptItem>()
            var subtotal = 0.0
            
            assignedItems.forEach { assignedItem ->
                when (val assignment = assignedItem.assignment) {
                    is ItemAssignment.IndividualAssignment -> {
                        if (assignment.participantId == participant.id) {
                            itemsOwed.add(assignedItem.receiptItem)
                            subtotal += assignedItem.receiptItem.cost
                        }
                    }
                    is ItemAssignment.EqualSplit -> {
                        if (assignment.participantIds.contains(participant.id)) {
                            val splitCost = assignedItem.receiptItem.cost / assignment.participantIds.size
                            val splitItem = assignedItem.receiptItem.copy(
                                name = "${assignedItem.receiptItem.name} (split ${assignment.participantIds.size} ways)",
                                cost = splitCost
                            )
                            itemsOwed.add(splitItem)
                            subtotal += splitCost
                        }
                    }
                    ItemAssignment.Unassigned -> {
                        // Unassigned items don't contribute to individual balances
                    }
                }
            }
            
            // Service charge is split equally among all participants
            val participantServiceCharge = if (totalParticipants > 0) serviceCharge / totalParticipants else 0.0
            val total = subtotal + participantServiceCharge
            
            return ParticipantBalance(
                participant = participant,
                itemsOwed = itemsOwed,
                subtotal = subtotal,
                serviceCharge = participantServiceCharge,
                total = total
            )
        }
    }
}

data class PaymentSummary(
    val payer: Participant,
    val totalBillAmount: Double,
    val payerOwes: Double,
    val payments: List<Payment>
) {
    companion object {
        fun calculate(
            participants: List<Participant>,
            balances: List<ParticipantBalance>,
            payerId: String
        ): PaymentSummary? {
            val payer = participants.find { it.id == payerId } ?: return null
            val payerBalance = balances.find { it.participant.id == payerId } ?: return null
            
            val totalBillAmount = balances.sumOf { it.total }
            val payerOwes = payerBalance.total
            
            val payments = balances
                .filter { it.participant.id != payerId && it.total > 0 }
                .map { balance ->
                    Payment(
                        from = balance.participant,
                        to = payer,
                        amount = balance.total
                    )
                }
            
            return PaymentSummary(
                payer = payer,
                totalBillAmount = totalBillAmount,
                payerOwes = payerOwes,
                payments = payments
            )
        }
    }
}

data class Payment(
    val from: Participant,
    val to: Participant,
    val amount: Double
)

data class EditableReceiptWithSplitting(
    val editableReceipt: EditableReceipt,
    val participants: List<Participant>,
    val assignedItems: List<AssignedReceiptItem>,
    val billSplitSummary: BillSplitSummary
) {
    companion object {
        fun fromEditableReceipt(editableReceipt: EditableReceipt): EditableReceiptWithSplitting {
            val assignedItems = editableReceipt.items.map { item ->
                AssignedReceiptItem(item, ItemAssignment.Unassigned)
            }
            val participants = emptyList<Participant>()
            val billSplitSummary = BillSplitSummary.calculate(participants, assignedItems, editableReceipt.serviceCharge)
            
            return EditableReceiptWithSplitting(
                editableReceipt = editableReceipt,
                participants = participants,
                assignedItems = assignedItems,
                billSplitSummary = billSplitSummary
            )
        }
    }
    
    fun addParticipant(name: String): EditableReceiptWithSplitting {
        val newParticipant = Participant.create(name)
        val updatedParticipants = participants + newParticipant
        val updatedSummary = BillSplitSummary.calculate(updatedParticipants, assignedItems, editableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            participants = updatedParticipants,
            billSplitSummary = updatedSummary
        )
    }
    
    fun removeParticipant(participantId: String): EditableReceiptWithSplitting {
        val updatedParticipants = participants.filter { it.id != participantId }
        // Remove participant from any assignments
        val updatedAssignedItems = assignedItems.map { assignedItem ->
            when (val assignment = assignedItem.assignment) {
                is ItemAssignment.IndividualAssignment -> {
                    if (assignment.participantId == participantId) {
                        assignedItem.copy(assignment = ItemAssignment.Unassigned)
                    } else {
                        assignedItem
                    }
                }
                is ItemAssignment.EqualSplit -> {
                    val updatedParticipantIds = assignment.participantIds.filter { it != participantId }
                    if (updatedParticipantIds.isEmpty()) {
                        assignedItem.copy(assignment = ItemAssignment.Unassigned)
                    } else {
                        assignedItem.copy(assignment = ItemAssignment.EqualSplit(updatedParticipantIds))
                    }
                }
                ItemAssignment.Unassigned -> assignedItem
            }
        }
        // Clear payer if the removed participant was the payer
        val updatedPayerId = if (billSplitSummary.payerId == participantId) null else billSplitSummary.payerId
        val updatedSummary = BillSplitSummary.calculate(updatedParticipants, updatedAssignedItems, editableReceipt.serviceCharge, updatedPayerId)
        
        return copy(
            participants = updatedParticipants,
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun assignItemToParticipant(itemIndex: Int, participantId: String): EditableReceiptWithSplitting {
        if (itemIndex < 0 || itemIndex >= assignedItems.size) return this
        
        val updatedAssignedItems = assignedItems.toMutableList()
        updatedAssignedItems[itemIndex] = updatedAssignedItems[itemIndex].copy(
            assignment = ItemAssignment.IndividualAssignment(participantId)
        )
        val updatedSummary = BillSplitSummary.calculate(participants, updatedAssignedItems, editableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun assignItemToEqualSplit(itemIndex: Int, participantIds: List<String>): EditableReceiptWithSplitting {
        if (itemIndex < 0 || itemIndex >= assignedItems.size) return this
        
        val updatedAssignedItems = assignedItems.toMutableList()
        updatedAssignedItems[itemIndex] = updatedAssignedItems[itemIndex].copy(
            assignment = if (participantIds.isEmpty()) {
                ItemAssignment.Unassigned
            } else {
                ItemAssignment.EqualSplit(participantIds)
            }
        )
        val updatedSummary = BillSplitSummary.calculate(participants, updatedAssignedItems, editableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun updateReceiptItem(itemIndex: Int, newItem: ReceiptItem): EditableReceiptWithSplitting {
        val updatedEditableReceipt = editableReceipt.updateItem(itemIndex, newItem)
        val updatedAssignedItems = assignedItems.toMutableList()
        if (itemIndex >= 0 && itemIndex < updatedAssignedItems.size) {
            updatedAssignedItems[itemIndex] = updatedAssignedItems[itemIndex].copy(receiptItem = newItem)
        }
        val updatedSummary = BillSplitSummary.calculate(participants, updatedAssignedItems, updatedEditableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            editableReceipt = updatedEditableReceipt,
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun addReceiptItem(item: ReceiptItem): EditableReceiptWithSplitting {
        val updatedEditableReceipt = editableReceipt.addItem(item)
        val updatedAssignedItems = assignedItems + AssignedReceiptItem(item, ItemAssignment.Unassigned)
        val updatedSummary = BillSplitSummary.calculate(participants, updatedAssignedItems, updatedEditableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            editableReceipt = updatedEditableReceipt,
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun deleteReceiptItem(itemIndex: Int): EditableReceiptWithSplitting {
        val updatedEditableReceipt = editableReceipt.deleteItem(itemIndex)
        val updatedAssignedItems = assignedItems.toMutableList()
        if (itemIndex >= 0 && itemIndex < updatedAssignedItems.size) {
            updatedAssignedItems.removeAt(itemIndex)
        }
        val updatedSummary = BillSplitSummary.calculate(participants, updatedAssignedItems, updatedEditableReceipt.serviceCharge, billSplitSummary.payerId)
        
        return copy(
            editableReceipt = updatedEditableReceipt,
            assignedItems = updatedAssignedItems,
            billSplitSummary = updatedSummary
        )
    }
    
    fun designatePayer(payerId: String): EditableReceiptWithSplitting {
        val updatedSummary = BillSplitSummary.calculate(participants, assignedItems, editableReceipt.serviceCharge, payerId)
        return copy(billSplitSummary = updatedSummary)
    }
    
    fun clearPayer(): EditableReceiptWithSplitting {
        val updatedSummary = BillSplitSummary.calculate(participants, assignedItems, editableReceipt.serviceCharge, null)
        return copy(billSplitSummary = updatedSummary)
    }
}
