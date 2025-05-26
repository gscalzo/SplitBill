package uk.co.effectivecode.firebender.splitbill.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.effectivecode.firebender.splitbill.BuildConfig
import uk.co.effectivecode.firebender.splitbill.data.*
import uk.co.effectivecode.firebender.splitbill.network.OpenAIClient
import uk.co.effectivecode.firebender.splitbill.utils.ImageUtils

interface ReceiptParsingService {
    suspend fun parseReceipt(context: Context, imageUri: Uri): Result<ReceiptParseResult>
}

class OpenAIService(
    private val apiKey: String = BuildConfig.OPENAI_API_KEY,
    private val useMock: Boolean = false
) : ReceiptParsingService {
    
    private val gson = Gson()
    
    override suspend fun parseReceipt(context: Context, imageUri: Uri): Result<ReceiptParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API key is configured
                if (apiKey.isBlank() && !useMock) {
                    return@withContext Result.failure(Exception("OpenAI API key not configured. Please add OPENAI_API_KEY to your .env file."))
                }
                
                // Resize and encode image
                val base64Image = ImageUtils.resizeAndEncodeImage(context, imageUri)
                    ?: return@withContext Result.failure(Exception("Failed to process image"))
                
                if (useMock) {
                    // Return mock data for testing
                    kotlinx.coroutines.delay(2000) // Simulate API delay
                    val mockResult = createMockResponse()
                    Result.success(mockResult)
                } else {
                    // Make actual API call
                    makeOpenAIApiCall(base64Image)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun makeOpenAIApiCall(base64Image: String): Result<ReceiptParseResult> {
        return try {
            // Create OpenAI request with structured output schema
            val request = createOpenAIRequest(base64Image)
            
            // Log the request for debugging (only in debug builds)
            if (BuildConfig.DEBUG) {
                val requestJson = gson.toJson(request)
                println("OpenAI Request: $requestJson")
            }
            
            // Make API call
            val response = OpenAIClient.api.parseReceipt("Bearer $apiKey", request = request)
            
            if (response.isSuccessful) {
                val openAIResponse = response.body()
                val content = openAIResponse?.choices?.firstOrNull()?.message?.content
                
                if (content != null) {
                    try {
                        val result = gson.fromJson(content, ReceiptParseResult::class.java)
                        Result.success(result)
                    } catch (e: Exception) {
                        Result.failure(Exception("Failed to parse API response: ${e.message}"))
                    }
                } else {
                    Result.failure(Exception("Empty response from OpenAI API"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("API call failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    private fun createOpenAIRequest(base64Image: String): OpenAIRequest {
        val schema = createReceiptParseSchema()
        
        return OpenAIRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAIMessage(
                    role = "user",
                    content = listOf(
                        MessageContent(
                            type = "text",
                            text = """
                                Analyze this image to determine if it's a UK expense receipt. 
                                
                                If it is NOT a valid receipt or the image is unclear, return:
                                {"error": "description of why it's not a valid receipt"}
                                
                                If it IS a valid UK receipt, extract the following information:
                                - items: array of {name: string, cost: number} for each line item
                                - service: number (service charge/tip if present, or null)
                                - total: number (final total amount)
                                
                                Return the data in the specified JSON structure. Use British pound values as numbers (e.g., 12.50 not "£12.50").
                            """.trimIndent()
                        ),
                        MessageContent(
                            type = "image_url",
                            imageUrl = ImageUrl("data:image/jpeg;base64,$base64Image")
                        )
                    )
                )
            ),
            responseFormat = ResponseFormat(
                jsonSchema = JsonSchema(schema = schema)
            )
        )
    }
    
    private fun createReceiptParseSchema(): Schema {
        return Schema(
            type = "object",
            properties = mapOf(
                "error" to Property(
                    type = "string",
                    description = "Error message if the image is invalid or not a receipt"
                ),
                "items" to Property(
                    type = "array",
                    description = "List of items found on the receipt",
                    items = Property(
                        type = "object",
                        properties = mapOf(
                            "name" to Property(type = "string", description = "Name of the item"),
                            "cost" to Property(type = "number", description = "Cost of the item")
                        ),
                        required = listOf("name", "cost"),
                        additionalProperties = false
                    )
                ),
                "service" to Property(
                    type = "number",
                    description = "Service charge if present"
                ),
                "total" to Property(
                    type = "number",
                    description = "Total amount on the receipt"
                )
            ),
            required = emptyList(),
            additionalProperties = false
        )
    }
    
    // Mock response for demo/testing purposes
    private fun createMockResponse(): ReceiptParseResult {
        return ReceiptParseResult(
            items = listOf(
                ReceiptItem("Margherita Pizza", 12.50),
                ReceiptItem("Caesar Salad", 8.75),
                ReceiptItem("Coca Cola", 2.95)
            ),
            service = 2.41,
            total = 26.61
        )
    }
}

// Mock implementation for testing
class MockReceiptParsingService : ReceiptParsingService {
    override suspend fun parseReceipt(context: Context, imageUri: Uri): Result<ReceiptParseResult> {
        return withContext(Dispatchers.IO) {
            // Simulate processing delay
            kotlinx.coroutines.delay(2000)
            
            Result.success(
                ReceiptParseResult(
                    items = listOf(
                        ReceiptItem("Fish & Chips", 8.95),
                        ReceiptItem("Mushy Peas", 2.50),
                        ReceiptItem("Tea", 1.80)
                    ),
                    service = 1.33,
                    total = 14.58
                )
            )
        }
    }
}
