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
                
                // Encode image without resizing for better OCR accuracy
                val base64Image = ImageUtils.encodeImage(context, imageUri, quality = 90)
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
            
            if (BuildConfig.DEBUG) {
                val requestJson = gson.toJson(request)
                println("=== FULL OpenAI Request ===")
                println(requestJson)
                
                // Also log just the schema part for easier debugging
                val schemaJson = gson.toJson(request.responseFormat.jsonSchema.schema)
                println("=== Schema Only ===")
                println(schemaJson)
                println("======================")
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
                                
                                You must always return a JSON object with ALL four fields: error, items, service, total
                                
                                If it is NOT a valid receipt or the image is unclear:
                                {"error": "description of why it's not a valid receipt", "items": null, "service": null, "total": null}
                                
                                If it IS a valid UK receipt:
                                {"error": null, "items": [{"name": "Pizza", "quantity": 2, "cost": 20.00}, {"name": "Espresso", "quantity": 3, "cost": 9.00}], "service": 2.90, "total": 31.90}
                                
                                Rules for parsing items:
                                - Look for quantity indicators like "2x Pizza", "3 Espresso", "2 × Item"
                                - If no quantity is specified, default to 1
                                - The cost should be the TOTAL cost for that quantity
                                - Examples:
                                  * "2 Pizza £20" → {"name": "Pizza", "quantity": 2, "cost": 20.00}
                                  * "Espresso £3" → {"name": "Espresso", "quantity": 1, "cost": 3.00}
                                  * "3x Coffee £9" → {"name": "Coffee", "quantity": 3, "cost": 9.00}
                                
                                General rules:
                                - Always include all four fields (error, items, service, total)
                                - Use null for fields that don't apply
                                - Use British pound values as numbers (e.g., 12.50 not "£12.50")
                                - If no service charge, set service to null
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
                    type = listOf("string", "null"), // Union type with null
                    description = "Error message if the image is invalid or not a receipt"
                ),
                "items" to Property(
                    type = listOf("array", "null"), // Union type with null
                    description = "List of items found on the receipt",
                    items = Property(
                        type = "object",
                        properties = mapOf(
                            "name" to Property(type = "string", description = "Name of the item"),
                            "quantity" to Property(type = "integer", description = "Quantity of the item (default 1 if not specified)"),
                            "cost" to Property(type = "number", description = "Total cost for this quantity of the item")
                        ),
                        required = listOf("name", "quantity", "cost"),
                        additionalProperties = false
                    )
                ),
                "service" to Property(
                    type = listOf("number", "null"), // Union type with null
                    description = "Service charge if present"
                ),
                "total" to Property(
                    type = listOf("number", "null"), // Union type with null
                    description = "Total amount on the receipt"
                )
            ),
            required = listOf("error", "items", "service", "total"), // All fields must be required
            additionalProperties = false
        )
    }
    
    // Mock response for demo/testing purposes
    private fun createMockResponse(): ReceiptParseResult {
        return ReceiptParseResult(
            items = listOf(
                ReceiptItem("Margherita Pizza", 2, 25.00),
                ReceiptItem("Caesar Salad", 1, 8.75),
                ReceiptItem("Coca Cola", 3, 8.85)
            ),
            service = 4.26,
            total = 46.86
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
                        ReceiptItem("Fish & Chips", 2, 17.90),
                        ReceiptItem("Mushy Peas", 1, 2.50),
                        ReceiptItem("Tea", 2, 3.60)
                    ),
                    service = 2.40,
                    total = 26.40
                )
            )
        }
    }
}
