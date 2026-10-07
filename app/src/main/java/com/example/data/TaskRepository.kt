package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Board
import com.example.model.Task
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaskRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing tasks.")
    }

    fun observeTasks(userId: String): Flow<List<Task>> {
        val path = "tasks"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy({ it.order }, { it.createdAt?.seconds ?: 0L }))
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeBoards(userId: String): Flow<List<Board>> {
        val path = "boards"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Board::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getTaskById(taskId: String): Result<Task> {
        val docRef = db.collection("tasks").document(taskId)
        return try {
            val snapshot = docRef.get().await()
            val task = snapshot.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (task != null) {
                Result.success(task)
            } else {
                Result.failure(NoSuchElementException("Task not found"))
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTask(
        title: String,
        description: String = "",
        boardId: String = "default_board",
        status: String = "todo",
        priority: String = "medium",
        order: Int = 0,
        tags: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            id = taskId,
            userId = uid,
            boardId = boardId,
            title = title,
            description = description,
            status = status,
            priority = priority,
            order = order,
            tags = tags,
            isCompleted = status == "done"
        )
        val docRef = db.collection("tasks").document(taskId)
        return try {
            docRef.set(task.toCreateMap()).await()
            Result.success(taskId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        val docRef = db.collection("tasks").document(task.id)
        return try {
            docRef.update(task.toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatusAndOrder(
        taskId: String,
        newStatus: String,
        newOrder: Int
    ): Result<Unit> {
        val docRef = db.collection("tasks").document(taskId)
        val updates = mapOf<String, Any>(
            "status" to newStatus,
            "order" to newOrder,
            "isCompleted" to (newStatus == "done"),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean): Result<Unit> {
        val docRef = db.collection("tasks").document(taskId)
        val newStatus = if (completed) "done" else "todo"
        val updates = mapOf<String, Any>(
            "isCompleted" to completed,
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val docRef = db.collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createBoard(
        title: String,
        description: String = "",
        color: String = "#6366F1",
        icon: String = "clipboard"
    ): Result<String> {
        val uid = requireUserId()
        val boardId = UUID.randomUUID().toString()
        val board = Board(
            id = boardId,
            userId = uid,
            title = title,
            description = description,
            color = color,
            icon = icon
        )
        val docRef = db.collection("boards").document(boardId)
        return try {
            docRef.set(board.toCreateMap()).await()
            Result.success(boardId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBoard(boardId: String): Result<Unit> {
        val docRef = db.collection("boards").document(boardId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
