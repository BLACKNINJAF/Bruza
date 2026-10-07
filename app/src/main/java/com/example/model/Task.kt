package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class Task(
    val id: String = "",
    val userId: String = "",
    val boardId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "todo", // "todo", "in_progress", "review", "done"
    val priority: String = "medium", // "low", "medium", "high", "urgent"
    val order: Int = 0,
    val dueDate: Timestamp? = null,
    val tags: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "userId" to userId,
            "boardId" to boardId,
            "title" to title,
            "description" to description,
            "status" to status,
            "priority" to priority,
            "order" to order,
            "tags" to tags,
            "isCompleted" to isCompleted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (dueDate != null) {
            map["dueDate"] = dueDate
        }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "boardId" to boardId,
            "title" to title,
            "description" to description,
            "status" to status,
            "priority" to priority,
            "order" to order,
            "tags" to tags,
            "isCompleted" to isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (dueDate != null) {
            map["dueDate"] = dueDate
        }
        return map
    }
}
