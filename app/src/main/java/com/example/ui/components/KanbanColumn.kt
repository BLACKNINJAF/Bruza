package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusReview
import com.example.ui.theme.StatusTodo

data class ColumnDef(
    val key: String,
    val title: String,
    val color: Color
)

val DEFAULT_COLUMNS = listOf(
    ColumnDef("todo", "To Do", StatusTodo),
    ColumnDef("in_progress", "In Progress", StatusInProgress),
    ColumnDef("review", "In Review", StatusReview),
    ColumnDef("done", "Done", StatusDone)
)

@Composable
fun KanbanColumn(
    columnDef: ColumnDef,
    tasks: List<Task>,
    onStatusChange: (taskId: String, newStatus: String) -> Unit,
    onToggleCompletion: (taskId: String, completed: Boolean) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddTaskToColumn: (status: String) -> Unit,
    onDropToNextColumn: (Task) -> Unit,
    onDropToPrevColumn: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("kanban_column_${columnDef.key}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(columnDef.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = columnDef.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = columnDef.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tasks.size.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = columnDef.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onAddTaskToColumn(columnDef.key) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("add_task_to_${columnDef.key}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add task to ${columnDef.title}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks in ${columnDef.title}\nDrop or add one here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DragAndDropTaskCard(
                            task = task,
                            onStatusChange = { newStatus -> onStatusChange(task.id, newStatus) },
                            onToggleCompletion = { completed -> onToggleCompletion(task.id, completed) },
                            onEditClick = { onEditTask(task) },
                            onDeleteClick = { onDeleteTask(task.id) },
                            onDropToNextColumn = { onDropToNextColumn(task) },
                            onDropToPrevColumn = { onDropToPrevColumn(task) }
                        )
                    }
                }
            }
        }
    }
}
