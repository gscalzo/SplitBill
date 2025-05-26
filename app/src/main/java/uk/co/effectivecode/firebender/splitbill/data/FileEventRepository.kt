package uk.co.effectivecode.firebender.splitbill.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class FileEventRepository(private val context: Context) : EventRepository {
    
    private val gson = Gson()
    private val eventsDir: File
        get() {
            val dir = File(context.filesDir, "events")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
    
    override suspend fun saveEvent(event: BillEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "${event.id}.json")
            FileWriter(file).use { writer ->
                gson.toJson(event, writer)
                writer.flush() // Ensure data is written to disk
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllEvents(): Result<List<BillEvent>> = withContext(Dispatchers.IO) {
        try {
            val eventFiles = eventsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            val events = eventFiles.mapNotNull { file ->
                try {
                    FileReader(file).use {
                        gson.fromJson(it, BillEvent::class.java)
                    }
                } catch (e: Exception) {
                    // Log error or handle corrupted file
                    null
                }
            }.sortedByDescending { it.timestamp }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getEventById(id: String): Result<BillEvent?> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                FileReader(file).use {
                    Result.success(gson.fromJson(it, BillEvent::class.java))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEventName(id: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                val event = FileReader(file).use {
                    gson.fromJson(it, BillEvent::class.java)
                }
                event.name = newName // Assuming BillEvent.name is var
                FileWriter(file).use { writer ->
                    gson.toJson(event, writer)
                    writer.flush() // Ensure data is written to disk
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(eventsDir, "$id.json")
            if (file.exists()) {
                if (file.delete()) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete event file"))
                }
            } else {
                Result.success(Unit) // Event not found, considered success
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
