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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { NandanApp(this) }
    }
}

private enum class Workspace(val label: String) { ASSISTANT("Assistant"), TASKS("Tasks"), MEMORY("Memory"), REMINDERS("Reminders") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NandanApp(context: Context) {
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
    var info by remember { mutableStateOf("Your private workspace is stored on this device.") }

    suspend fun refresh() {
        tasks = dao.tasks()
        memories = dao.memories()
        reminders = dao.reminders()
    }
    LaunchedEffect(selected) { refresh() }

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Column { Text("Nandan"); Text("PERSONAL AI OS", style = MaterialTheme.typography.labelSmall) } }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                ScrollableTabRow(selectedTabIndex = selected.ordinal) {
                    Workspace.entries.forEach { item -> Tab(selected == item, { selected = item }, text = { Text(item.label) }) }
                }
                Text(info, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 10.dp))
                when (selected) {
                    Workspace.ASSISTANT -> AssistantWorkspace()
                    Workspace.TASKS -> {
                        OutlinedTextField(taskTitle, { taskTitle = it }, label = { Text("New task") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { if (taskTitle.isNotBlank()) scope.launch { dao.addTask(TaskEntity(title = taskTitle.trim())); taskTitle = ""; refresh() } }, modifier = Modifier.padding(top = 8.dp)) { Text("Add task") }
                        LazyColumn(Modifier.weight(1f)) {
                            items(tasks, key = { it.id }) { task ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(Modifier.weight(1f)) {
                                        Checkbox(task.completed, { checked -> scope.launch { dao.setTaskCompleted(task.id, checked); refresh() } })
                                        Text(task.title, modifier = Modifier.padding(top = 12.dp))
                                    }
                                    TextButton(onClick = { scope.launch { dao.deleteTask(task.id); refresh() } }) { Text("Delete") }
                                }
                            }
                        }
                    }
                    Workspace.MEMORY -> {
                        OutlinedTextField(memoryTitle, { memoryTitle = it }, label = { Text("Memory title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(memoryContent, { memoryContent = it }, label = { Text("What should Nandan remember?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(onClick = { if (memoryTitle.isNotBlank() && memoryContent.isNotBlank()) scope.launch { dao.addMemory(MemoryEntity(title = memoryTitle.trim(), content = memoryContent.trim())); memoryTitle = ""; memoryContent = ""; refresh() } }, modifier = Modifier.padding(top = 8.dp)) { Text("Save memory") }
                        LazyColumn(Modifier.weight(1f)) {
                            items(memories, key = { it.id }) { memory ->
                                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(memory.title, style = MaterialTheme.typography.titleMedium)
                                        Text(memory.content)
                                        TextButton(onClick = { scope.launch { dao.deleteMemory(memory.id); refresh() } }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                    Workspace.REMINDERS -> {
                        OutlinedTextField(reminderTitle, { reminderTitle = it }, label = { Text("Reminder title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(reminderNote, { reminderNote = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(reminderMinutes, { reminderMinutes = it.filter { char -> char.isDigit() }.take(5) }, label = { Text("Minutes from now") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val minutes = reminderMinutes.toLongOrNull()
                            if (reminderTitle.isNotBlank() && minutes != null && minutes > 0) scope.launch {
                                val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                                val title = reminderTitle.trim()
                                val note = reminderNote.trim()
                                val id = dao.addReminder(ReminderEntity(title = title, note = note, triggerAt = triggerAt))
                                scheduleReminder(context, id, title, note, triggerAt)
                                reminderTitle = ""; reminderNote = ""; refresh()
                                info = "Reminder scheduled on this device."
                            }
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Schedule reminder") }
                        LazyColumn(Modifier.weight(1f)) {
                            items(reminders, key = { it.id }) { reminder ->
                                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                                        Text(DateFormat.getDateTimeInstance().format(Date(reminder.triggerAt)))
                                        Text(if (reminder.acknowledged) "Acknowledged" else "Scheduled")
                                        Row {
                                            if (!reminder.acknowledged) TextButton(onClick = { cancelReminder(context, reminder.id); scope.launch { dao.acknowledgeReminder(reminder.id); refresh() } }) { Text("Done") }
                                            TextButton(onClick = { cancelReminder(context, reminder.id); scope.launch { dao.deleteReminder(reminder.id); refresh() } }) { Text("Delete") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantWorkspace() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nandan Android foundation", style = MaterialTheme.typography.headlineSmall)
        Text("Your tasks, memories and reminders are saved locally on this device.")
        ElevatedCard { Column(Modifier.padding(16.dp)) {
            Text("Local AI runtime", style = MaterialTheme.typography.titleMedium)
            Text("Not connected yet. The on-device model runtime will be added and benchmarked separately. No cloud AI service is silently substituted.")
        } }
        Text("Next: device pairing, local model integration and approved automation.")
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
    val pending = PendingIntent.getBroadcast(
        context, id.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarms.cancel(pending)
    pending.cancel()
}
