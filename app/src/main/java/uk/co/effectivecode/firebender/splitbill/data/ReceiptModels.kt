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
