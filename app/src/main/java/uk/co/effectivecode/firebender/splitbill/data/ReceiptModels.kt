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
