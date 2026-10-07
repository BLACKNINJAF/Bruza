package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TaskRepository
import com.example.model.Board
import com.example.model.SyncInfo
import com.example.model.SyncState
import com.example.model.Task
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "TaskViewModel"

enum class ViewMode {
    KANBAN,
    LIST
}

class TaskViewModel(
    private val repository: TaskRepository,
    val currentUserId: String,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _selectedBoardId = MutableStateFlow<String?>(null)
    val selectedBoardId = _selectedBoardId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _priorityFilter = MutableStateFlow<String?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.KANBAN)
    val viewMode = _viewMode.asStateFlow()

    private val _syncInfo = MutableStateFlow(SyncInfo())
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    val boards: StateFlow<List<Board>> = repository.observeBoards(currentUserId)
        .catch { error ->
            Log.e(TAG, "Error observing boards", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<Task>> = repository.observeTasks(currentUserId)
        .catch { error ->
            Log.e(TAG, "Error observing tasks", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Filtered tasks based on search, selected board, priority
    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        selectedBoardId,
        searchQuery,
        priorityFilter
    ) { tasks, boardId, query, priority ->
        tasks.filter { task ->
            val matchesBoard = boardId == null || task.boardId == boardId
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.tags.any { it.contains(query, ignoreCase = true) }
            val matchesPriority = priority == null || task.priority.equals(priority, ignoreCase = true)

            matchesBoard && matchesQuery && matchesPriority
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        // Monitor network state for sync indicators
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _syncInfo.value = _syncInfo.value.copy(
                    isOnline = online,
                    state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                    lastSyncTime = System.currentTimeMillis()
                )
            }
        }

        // Auto-select board or seed default workspace if none exists
        viewModelScope.launch {
            boards.collect { boardList ->
                if (boardList.isEmpty()) {
                    seedDefaultWorkspace()
                } else if (_selectedBoardId.value == null) {
                    _selectedBoardId.value = boardList.first().id
                }
            }
        }
    }

    private fun seedDefaultWorkspace() {
        viewModelScope.launch {
            val boardResult = repository.createBoard(
                title = "Main Workspace",
                description = "Primary task board for active projects",
                color = "#6366F1",
                icon = "clipboard"
            )
            if (boardResult.isSuccess) {
                val boardId = boardResult.getOrThrow()
                _selectedBoardId.value = boardId

                // Add starter tasks demonstrating drag-and-drop & status organization
                repository.createTask(
                    title = "🚀 Welcome to TaskFlow",
                    description = "Intuitive task management with real-time cloud sync, drag & drop, and offline access.",
                    boardId = boardId,
                    status = "todo",
                    priority = "high",
                    order = 0,
                    tags = listOf("Starter", "Guide")
                )
                repository.createTask(
                    title = "🖐️ Try Drag & Drop",
                    description = "Long press cards or use the quick column chips to organize across To Do, In Progress, Review, and Done.",
                    boardId = boardId,
                    status = "in_progress",
                    priority = "urgent",
                    order = 0,
                    tags = listOf("Features")
                )
                repository.createTask(
                    title = "📴 Test Offline Mode",
                    description = "Turn off Wi-Fi or airplane mode. You can edit and create tasks offline; they will sync automatically when back online.",
                    boardId = boardId,
                    status = "review",
                    priority = "medium",
                    order = 0,
                    tags = listOf("Offline")
                )
                repository.createTask(
                    title = "✅ Real-Time Sync Ready",
                    description = "Changes reflect instantly across all connected sessions and platforms.",
                    boardId = boardId,
                    status = "done",
                    priority = "low",
                    order = 0,
                    tags = listOf("Sync")
                )
            }
        }
    }

    fun selectBoard(boardId: String) {
        _selectedBoardId.value = boardId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPriorityFilter(priority: String?) {
        _priorityFilter.value = if (_priorityFilter.value == priority) null else priority
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun createTask(
        title: String,
        description: String = "",
        status: String = "todo",
        priority: String = "medium",
        tags: List<String> = emptyList()
    ) {
        val boardId = _selectedBoardId.value ?: "default_board"
        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.SYNCING)
            val result = repository.createTask(
                title = title,
                description = description,
                boardId = boardId,
                status = status,
                priority = priority,
                tags = tags
            )
            val online = _syncInfo.value.isOnline
            _syncInfo.value = _syncInfo.value.copy(
                state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                lastSyncTime = System.currentTimeMillis()
            )
            if (result.isFailure) {
                Log.e(TAG, "Failed to create task", result.exceptionOrNull())
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.SYNCING)
            repository.updateTask(task)
            val online = _syncInfo.value.isOnline
            _syncInfo.value = _syncInfo.value.copy(
                state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun moveTask(taskId: String, newStatus: String, newOrder: Int = 0) {
        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.SYNCING)
            repository.updateTaskStatusAndOrder(taskId, newStatus, newOrder)
            val online = _syncInfo.value.isOnline
            _syncInfo.value = _syncInfo.value.copy(
                state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun toggleTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.SYNCING)
            repository.toggleTaskCompletion(taskId, completed)
            val online = _syncInfo.value.isOnline
            _syncInfo.value = _syncInfo.value.copy(
                state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.SYNCING)
            repository.deleteTask(taskId)
            val online = _syncInfo.value.isOnline
            _syncInfo.value = _syncInfo.value.copy(
                state = if (online) SyncState.ONLINE_SYNCED else SyncState.OFFLINE_CACHED,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun createBoard(title: String, description: String = "", color: String = "#6366F1") {
        viewModelScope.launch {
            val result = repository.createBoard(
                title = title,
                description = description,
                color = color
            )
            if (result.isSuccess) {
                _selectedBoardId.value = result.getOrThrow()
            }
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            repository.deleteBoard(boardId)
            val remaining = boards.value.filter { it.id != boardId }
            _selectedBoardId.value = remaining.firstOrNull()?.id
        }
    }
}
