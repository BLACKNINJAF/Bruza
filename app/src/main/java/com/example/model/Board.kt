package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class Board(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val color: String = "#6366F1",
    val icon: String = "clipboard",
    val columns: List<String> = listOf("todo", "in_progress", "review", "done"),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "description" to description,
            "color" to color,
            "icon" to icon,
            "columns" to columns,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "description" to description,
            "color" to color,
            "icon" to icon,
            "columns" to columns,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
