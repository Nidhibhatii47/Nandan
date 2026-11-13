package com.aventaro.nandan

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { NandanLaunchFlow(this) }
    }
}

private enum class Workspace(val label: String) {
    ASSISTANT("Overview"), TASKS("Tasks"), MEMORY("Memory"), REMINDERS("Reminders")
}

@Composable
private fun NandanLaunchFlow(context: Context) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1750); showSplash = false }
    if (showSplash) NandanOmSplash() else NandanTabletApp(context)
}

@Composable
private fun NandanOmSplash() {
    val scale = remember { Animatable(4.2f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, animationSpec = tween(durationMillis = 1350)) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("ॐ", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            Text("NANDAN", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NandanTabletApp(context: Context) {
    val dao = remember { NandanDatabase.get(context).dao() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(Workspace.ASSISTANT) }
    var tasks by remember { mutableStateOf(emptyList<TaskEntity>()) }
    var memories by remember { mutableStateOf(emptyList<MemoryEntity>()) }
    var reminders by remember { mutableStateOf(emptyList<ReminderEntity>()) }
    var taskTitle by remember { mutableStateOf("") }
    var memoryTitle by remember { mutableStateOf("") }
    var memoryContent by remember { mutableStateOf("") }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderNote by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf("5") }
    var info by remember { mutableStateOf("All information is stored locally on this tablet.") }

    suspend fun refresh() {
        tasks = dao.tasks()
        memories = dao.memories()
        reminders = dao.reminders()
    }
    LaunchedEffect(selected) { refresh() }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("ॐ", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("NANDAN", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    NavigationRailItem(
                        selected = selected == Workspace.ASSISTANT,
                        onClick = { selected = Workspace.ASSISTANT },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Overview") }
                    )
                    NavigationRailItem(
                        selected = selected == Workspace.TASKS,
                        onClick = { selected = Workspace.TASKS },
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                        label = { Text("Tasks") }
                    )
                    NavigationRailItem(
                        selected = selected == Workspace.MEMORY,
                        onClick = { selected = Workspace.MEMORY },
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        label = { Text("Memory") }
                    )
                    NavigationRailItem(
                        selected = selected == Workspace.REMINDERS,
                        onClick = { selected = Workspace.REMINDERS },
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                        label = { Text("Reminders") }
                    )
                    Spacer(Modifier.weight(1f))
                    Text("LOCAL", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 20.dp), color = MaterialTheme.colorScheme.secondary)
                }
                VerticalDivider()
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 28.dp, vertical = 22.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Nandan", style = MaterialTheme.typography.headlineMedium)
                            Text("PERSONAL AI WORKSPACE  /  TABLET EDITION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        VoiceCommandButton(onCommand = { spoken ->\n                            scope.launch {\n                                val command = spoken.trim()\n                                val lower = command.lowercase()\n                                when {\n                                    lower.startsWith("add task ") || lower.startsWith("create task ") -> {\n                                        val title = command.substringAfter("task", "").trim()\n                                        if (title.isNotBlank()) { dao.addTask(TaskEntity(title = title)); refresh(); info = "Task added: $title" }\n                                    }\n                                    lower.startsWith("remember ") || lower.startsWith("save memory ") -> {\n                                        val content = if (lower.startsWith("remember ")) command.substringAfter("remember").trim() else command.substringAfter("save memory").trim()\n                                        if (content.isNotBlank()) { dao.addMemory(MemoryEntity(title = "Voice memory", content = content)); refresh(); info = "Memory saved." }\n                                    }\n                                    Regex("remind me in (\\\\d+) minutes? (to )?(.+)", RegexOption.IGNORE_CASE).matches(command) -> {\n                                        val match = Regex("remind me in (\\\\d+) minutes? (to )?(.+)", RegexOption.IGNORE_CASE).find(command)!!\n                                        val minutes = match.groupValues[1].toLong()\n                                        val title = match.groupValues[3].trim()\n                                        val at = System.currentTimeMillis() + minutes * 60_000L\n                                        val id = dao.addReminder(ReminderEntity(title = title, triggerAt = at))\n                                        scheduleReminder(context, id, title, "", at); refresh(); info = "Reminder set for $minutes minutes: $title"\n                                    }\n                                    lower.contains("open tasks") || lower.contains("show tasks") -> { selected = Workspace.TASKS; info = "Opening tasks." }\n                                    lower.contains("open memory") || lower.contains("show memories") -> { selected = Workspace.MEMORY; info = "Opening memory." }\n                                    lower.contains("open reminders") || lower.contains("show reminders") -> { selected = Workspace.REMINDERS; info = "Opening reminders." }\n                                    lower.contains("open overview") || lower.contains("go home") -> { selected = Workspace.ASSISTANT; info = "Opening overview." }\n                                    else -> info = "I heard: \\$command. Try: add task…, remember…, remind me in 5 minutes to…, or open tasks."\n                                }\n                            }\n                        }, onStatus = { info = it })
                    }
                    Text(info, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp, bottom = 18.dp))
                    when (selected) {
                        Workspace.ASSISTANT -> OverviewWorkspace(tasks.size, memories.size, reminders.count { !it.acknowledged })
                        Workspace.TASKS -> TasksWorkspace(tasks, taskTitle, { taskTitle = it }, {
                            if (taskTitle.isNotBlank()) scope.launch {
                                dao.addTask(TaskEntity(title = taskTitle.trim()))
                                taskTitle = ""
                                refresh()
                            }
                        }, { task, checked -> scope.launch { dao.setTaskCompleted(task.id, checked); refresh() } }, { task ->
                            scope.launch { dao.deleteTask(task.id); refresh() }
                        })
                        Workspace.MEMORY -> MemoryWorkspace(memories, memoryTitle, { memoryTitle = it }, memoryContent, { memoryContent = it }, {
                            if (memoryTitle.isNotBlank() && memoryContent.isNotBlank()) scope.launch {
                                dao.addMemory(MemoryEntity(title = memoryTitle.trim(), content = memoryContent.trim()))
                                memoryTitle = ""; memoryContent = ""; refresh()
                            }
                        }, { memory -> scope.launch { dao.deleteMemory(memory.id); refresh() } })
                        Workspace.REMINDERS -> RemindersWorkspace(
                            reminders, reminderTitle, { reminderTitle = it }, reminderNote, { reminderNote = it },
                            reminderMinutes, { reminderMinutes = it.filter(Char::isDigit).take(5) },
                            {
                                val minutes = reminderMinutes.toLongOrNull()
                                if (reminderTitle.isNotBlank() && minutes != null && minutes > 0) scope.launch {
                                    val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                                    val title = reminderTitle.trim()
                                    val note = reminderNote.trim()
                                    val id = dao.addReminder(ReminderEntity(title = title, note = note, triggerAt = triggerAt))
                                    scheduleReminder(context, id, title, note, triggerAt)
                                    reminderTitle = ""; reminderNote = ""; refresh()
                                    info = "Reminder scheduled on this tablet."
                                }
                            },
                            { reminder -> cancelReminder(context, reminder.id); scope.launch { dao.acknowledgeReminder(reminder.id); refresh() } },
                            { reminder -> cancelReminder(context, reminder.id); scope.launch { dao.deleteReminder(reminder.id); refresh() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewWorkspace(taskCount: Int, memoryCount: Int, reminderCount: Int) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Your command centre", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard("Open tasks", taskCount.toString(), Modifier.weight(1f))
            MetricCard("Saved memories", memoryCount.toString(), Modifier.weight(1f))
            MetricCard("Active reminders", reminderCount.toString(), Modifier.weight(1f))
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Local AI engine", style = MaterialTheme.typography.titleLarge)
                Text("The Android app foundation is ready. The on-device model runtime is the next engineering component and has not been connected yet.")
                Text("No cloud AI service is silently substituted.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text("Use the navigation rail to manage tasks, memories and reminders.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TasksWorkspace(
    tasks: List<TaskEntity>, title: String, onTitle: (String) -> Unit, onAdd: () -> Unit,
    onToggle: (TaskEntity, Boolean) -> Unit, onDelete: (TaskEntity) -> Unit
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ElevatedCard(Modifier.width(300.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create a task", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(title, onTitle, label = { Text("Task title") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { Text("Add task") }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Your task list", style = MaterialTheme.typography.titleLarge)
            if (tasks.isEmpty()) Text("No tasks yet. Add your first task.", modifier = Modifier.padding(top = 16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(tasks, key = { it.id }) { task ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(task.completed, { onToggle(task, it) })
                            Text(task.title, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onDelete(task) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryWorkspace(
    memories: List<MemoryEntity>, title: String, onTitle: (String) -> Unit,
    content: String, onContent: (String) -> Unit, onSave: () -> Unit, onDelete: (MemoryEntity) -> Unit
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ElevatedCard(Modifier.width(340.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save a memory", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(title, onTitle, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(content, onContent, label = { Text("Memory details") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank() && content.isNotBlank()) { Text("Save memory") }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Saved memories", style = MaterialTheme.typography.titleLarge)
            if (memories.isEmpty()) Text("Your saved information will appear here.", modifier = Modifier.padding(top = 16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(memories, key = { it.id }) { memory ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(memory.title, style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { onDelete(memory) }) { Text("Delete") }
                            }
                            Text(memory.content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersWorkspace(
    reminders: List<ReminderEntity>, title: String, onTitle: (String) -> Unit,
    note: String, onNote: (String) -> Unit, minutes: String, onMinutes: (String) -> Unit,
    onSchedule: () -> Unit, onDone: (ReminderEntity) -> Unit, onDelete: (ReminderEntity) -> Unit
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ElevatedCard(Modifier.width(340.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Schedule a reminder", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(title, onTitle, label = { Text("Reminder title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, onNote, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(minutes, onMinutes, label = { Text("Minutes from now") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = onSchedule, modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank() && (minutes.toLongOrNull() ?: 0L) > 0) { Text("Schedule reminder") }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Your reminders", style = MaterialTheme.typography.titleLarge)
            if (reminders.isEmpty()) Text("No reminders scheduled.", modifier = Modifier.padding(top = 16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(reminders, key = { it.id }) { reminder ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                                Text(if (reminder.acknowledged) "Completed" else "Scheduled", color = MaterialTheme.colorScheme.secondary)
                            }
                            Text(DateFormat.getDateTimeInstance().format(Date(reminder.triggerAt)))
                            if (reminder.note.isNotBlank()) Text(reminder.note)
                            Row {
                                if (!reminder.acknowledged) TextButton(onClick = { onDone(reminder) }) { Text("Done") }
                                TextButton(onClick = { onDelete(reminder) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun scheduleReminder(context: Context, id: Long, title: String, note: String, triggerAt: Long) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_ID, id.toInt())
        putExtra(ReminderReceiver.EXTRA_TITLE, title)
        putExtra(ReminderReceiver.EXTRA_NOTE, note)
    }
    val pending = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarms.canScheduleExactAlarms()) {
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    } else {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }
}

private fun cancelReminder(context: Context, id: Long) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarms.cancel(pending)
    pending.cancel()
}
