package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Task
import com.example.ui.components.BoardDialog
import com.example.ui.components.DEFAULT_COLUMNS
import com.example.ui.components.DragAndDropTaskCard
import com.example.ui.components.KanbanColumn
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.TaskEditDialog
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import com.example.viewmodel.TaskViewModel
import com.example.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    viewModel: TaskViewModel,
    onSignOutClick: () -> Unit
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val selectedBoardId by viewModel.selectedBoardId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val syncInfo by viewModel.syncInfo.collectAsStateWithLifecycle()

    var showBoardMenu by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskDefaultStatus by remember { mutableStateOf("todo") }
    var showNewBoardDialog by remember { mutableStateOf(false) }

    val currentBoard = boards.find { it.id == selectedBoardId } ?: boards.firstOrNull()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showBoardMenu = true }
                                .testTag("board_selector_dropdown")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(currentBoard?.color ?: "#6366F1"))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentBoard?.title ?: "Main Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Board",
                                modifier = Modifier.size(20.dp)
                            )

                            DropdownMenu(
                                expanded = showBoardMenu,
                                onDismissRequest = { showBoardMenu = false }
                            ) {
                                boards.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.title, fontWeight = if (b.id == selectedBoardId) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        try {
                                                            Color(android.graphics.Color.parseColor(b.color))
                                                        } catch (e: Exception) {
                                                            Color.Gray
                                                        }
                                                    )
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectBoard(b.id)
                                            showBoardMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ Create New Board", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showBoardMenu = false
                                        showNewBoardDialog = true
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(
                            onClick = { showSearchField = !showSearchField },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search Tasks")
                        }

                        // Kanban vs List Toggle
                        IconButton(
                            onClick = {
                                viewModel.setViewMode(
                                    if (viewMode == ViewMode.KANBAN) ViewMode.LIST else ViewMode.KANBAN
                                )
                            },
                            modifier = Modifier.testTag("view_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.KANBAN) Icons.Default.ViewList else Icons.Default.ViewKanban,
                                contentDescription = if (viewMode == ViewMode.KANBAN) "Switch to List View" else "Switch to Kanban View"
                            )
                        }

                        // Profile / Sign Out
                        Box {
                            IconButton(
                                onClick = { showUserMenu = true },
                                modifier = Modifier.testTag("user_menu_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "TF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showUserMenu,
                                onDismissRequest = { showUserMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                    onClick = {
                                        showUserMenu = false
                                        onSignOutClick()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Real-Time Sync Status Banner
                SyncStatusBanner(syncInfo = syncInfo)

                // Search Bar (Expandable)
                AnimatedVisibility(visible = showSearchField) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search title, description, or #tag...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_query_input")
                        )
                    }
                }

                // Filter Chips Bar (Priority & Counts)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilterChip(
                        selected = priorityFilter == null,
                        onClick = { viewModel.setPriorityFilter(null) },
                        label = { Text("All (${tasks.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_priority_all")
                    )

                    listOf(
                        "urgent" to ("Urgent" to PriorityUrgent),
                        "high" to ("High" to PriorityHigh),
                        "medium" to ("Medium" to PriorityMedium),
                        "low" to ("Low" to PriorityLow)
                    ).forEach { (key, pair) ->
                        val (label, color) = pair
                        FilterChip(
                            selected = priorityFilter == key,
                            onClick = { viewModel.setPriorityFilter(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            ),
                            modifier = Modifier.testTag("filter_priority_$key")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    taskDefaultStatus = "todo"
                    showTaskDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewMode == ViewMode.KANBAN) {
                // Horizontal Kanban Columns
                val horizontalScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DEFAULT_COLUMNS.forEachIndexed { index, col ->
                        val colTasks = tasks.filter { it.status == col.key }
                        KanbanColumn(
                            columnDef = col,
                            tasks = colTasks,
                            onStatusChange = { taskId, newStatus ->
                                viewModel.moveTask(taskId, newStatus)
                            },
                            onToggleCompletion = { taskId, completed ->
                                viewModel.toggleTask(taskId, completed)
                            },
                            onEditTask = { task ->
                                taskToEdit = task
                                showTaskDialog = true
                            },
                            onDeleteTask = { taskId ->
                                viewModel.deleteTask(taskId)
                            },
                            onAddTaskToColumn = { statusKey ->
                                taskToEdit = null
                                taskDefaultStatus = statusKey
                                showTaskDialog = true
                            },
                            onDropToNextColumn = { task ->
                                val nextColIndex = (index + 1).coerceAtMost(DEFAULT_COLUMNS.size - 1)
                                val nextStatus = DEFAULT_COLUMNS[nextColIndex].key
                                viewModel.moveTask(task.id, nextStatus)
                            },
                            onDropToPrevColumn = { task ->
                                val prevColIndex = (index - 1).coerceAtLeast(0)
                                val prevStatus = DEFAULT_COLUMNS[prevColIndex].key
                                viewModel.moveTask(task.id, prevStatus)
                            }
                        )
                    }
                }
            } else {
                // Grouped Clean List View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DragAndDropTaskCard(
                            task = task,
                            onStatusChange = { newStatus -> viewModel.moveTask(task.id, newStatus) },
                            onToggleCompletion = { completed -> viewModel.toggleTask(task.id, completed) },
                            onEditClick = {
                                taskToEdit = task
                                showTaskDialog = true
                            },
                            onDeleteClick = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // Task Creation / Editing Dialog
    if (showTaskDialog) {
        TaskEditDialog(
            initialTask = taskToEdit,
            initialStatus = taskDefaultStatus,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, status, priority, tags ->
                showTaskDialog = false
                val editing = taskToEdit
                if (editing == null) {
                    viewModel.createTask(
                        title = title,
                        description = desc,
                        status = status,
                        priority = priority,
                        tags = tags
                    )
                } else {
                    viewModel.updateTask(
                        editing.copy(
                            title = title,
                            description = desc,
                            status = status,
                            priority = priority,
                            tags = tags,
                            isCompleted = (status == "done")
                        )
                    )
                }
            }
        )
    }

    // Board Creation Dialog
    if (showNewBoardDialog) {
        BoardDialog(
            onDismiss = { showNewBoardDialog = false },
            onSave = { title, desc, color ->
                showNewBoardDialog = false
                viewModel.createBoard(title, desc, color)
            }
        )
    }
}
