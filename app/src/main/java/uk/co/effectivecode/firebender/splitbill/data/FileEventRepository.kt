package uk.co.effectivecode.firebender.splitbill.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class FileEventRepository(private val context: Context) : EventRepository {
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(ItemAssignment::class.java, ItemAssignmentTypeAdapter())
        .create()
        
    private val eventsDir: File
        get() {
            val dir = File(context.filesDir, "events")
            if (!dir.exists()) {
                Log.d(TAG, "Creating events directory: ${dir.absolutePath}")
                dir.mkdirs()
            }
            Log.d(TAG, "Events directory: ${dir.absolutePath}, exists: ${dir.exists()}")
            return dir
        }
    
    override suspend fun saveEvent(event: BillEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save event: ${event.id}, name: ${event.name}")
            val file = File(eventsDir, "${event.id}.json")
            Log.d(TAG, "Saving to file: ${file.absolutePath}")
            
            FileWriter(file).use { writer ->
                val jsonString = gson.toJson(event)
                Log.d(TAG, "JSON length: ${jsonString.length} characters")
                writer.write(jsonString)
                writer.flush() // Ensure data is written to disk
            }
            
            Log.d(TAG, "Event saved successfully. File exists: ${file.exists()}, File size: ${file.length()} bytes")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save event: ${event.id}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAllEvents(): Result<List<BillEvent>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading all events from directory: ${eventsDir.absolutePath}")
            val eventFiles = eventsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            Log.d(TAG, "Found ${eventFiles.size} event files: ${eventFiles.map { it.name }}")
            
            val events = eventFiles.mapNotNull { file ->
                try {
                    Log.d(TAG, "Reading event file: ${file.name}, size: ${file.length()} bytes")
                    FileReader(file).use {
                        val event = gson.fromJson(it, BillEvent::class.java)
                        Log.d(TAG, "Successfully loaded event: ${event.id}, name: ${event.name}")
                        event
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load event from file: ${file.name}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }
            
            Log.d(TAG, "Successfully loaded ${events.size} events")
            events.forEach { event ->
                Log.d(TAG, "Loaded event: ${event.id}, name: ${event.name}, timestamp: ${event.timestamp}")
            }
            
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load events", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getEventById(id: String): Result<BillEvent?> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                FileReader(file).use {
                    Log.d(TAG, "Loading event by ID: $id")
                    Result.success(gson.fromJson(it, BillEvent::class.java))
                }
            } else {
                Log.d(TAG, "Event not found for ID: $id")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load event by ID: $id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateEventName(id: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                Log.d(TAG, "Updating event name for ID: $id to $newName")
                val event = FileReader(file).use {
                    gson.fromJson(it, BillEvent::class.java)
                }
                event.name = newName // Assuming BillEvent.name is var
                FileWriter(file).use { writer ->
                    val jsonString = gson.toJson(event)
                    Log.d(TAG, "Updated event JSON length: ${jsonString.length} characters")
                    writer.write(jsonString)
                    writer.flush() // Ensure data is written to disk
                }
                Log.d(TAG, "Event name updated successfully for ID: $id")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Event not found for ID: $id during update")
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update event name for ID: $id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                Log.d(TAG, "Deleting event file for ID: $id")
                if (file.delete()) {
                    Log.d(TAG, "Event file deleted successfully for ID: $id")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Failed to delete event file for ID: $id")
                    Result.failure(Exception("Failed to delete event file"))
                }
            } else {
                Log.d(TAG, "Event file not found for ID: $id during deletion")
                Result.success(Unit) // Event not found, considered success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete event for ID: $id", e)
            Result.failure(e)
        }
    }
}

class ItemAssignmentTypeAdapter : TypeAdapter<ItemAssignment>() {
    override fun write(out: JsonWriter, value: ItemAssignment?) {
        if (value == null) {
            out.nullValue()
            return
        }
        
        out.beginObject()
        when (value) {
            is ItemAssignment.EqualSplit -> {
                out.name("type").value("EqualSplit")
                out.name("participantIds")
                out.beginArray()
                value.participantIds.forEach { out.value(it) }
                out.endArray()
            }
            is ItemAssignment.IndividualAssignment -> {
                out.name("type").value("IndividualAssignment")
                out.name("participantId").value(value.participantId)
            }
            is ItemAssignment.Unassigned -> {
                out.name("type").value("Unassigned")
            }
        }
        out.endObject()
    }
    
    override fun read(`in`: JsonReader): ItemAssignment? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        
        `in`.beginObject()
        var type: String? = null
        var participantIds: List<String>? = null
        var participantId: String? = null
        
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "type" -> type = `in`.nextString()
                "participantIds" -> {
                    participantIds = mutableListOf<String>().apply {
                        `in`.beginArray()
                        while (`in`.hasNext()) {
                            add(`in`.nextString())
                        }
                        `in`.endArray()
                    }
                }
                "participantId" -> participantId = `in`.nextString()
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()
        
        return when (type) {
            "EqualSplit" -> ItemAssignment.EqualSplit(participantIds ?: emptyList())
            "IndividualAssignment" -> ItemAssignment.IndividualAssignment(participantId ?: "")
            "Unassigned" -> ItemAssignment.Unassigned
            else -> ItemAssignment.Unassigned
        }
    }
}

private const val TAG = "FileEventRepository"
