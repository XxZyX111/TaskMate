package com.aqeela.taskmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF4263EB), secondary = Color(0xFF526071),
                background = Color(0xFFF6F7FB), surface = Color.White
            )) { TaskMateApp() }
        }
    }
}

@Composable
private fun TaskMateApp(vm: TaskMateViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        val text = vm.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.dismissMessage()
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!vm.configured) {
                Column(Modifier.align(Alignment.Center).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Welcome to TaskMate", style = MaterialTheme.typography.headlineMedium)
                    Text("One setup step remains: connect your Firebase project.")
                    Text("1. Register an Android app named com.aqeela.taskmate.\n" +
                        "2. Place google-services.json inside the app folder.\n" +
                        "3. Enable Email/Password in Firebase Authentication.\n" +
                        "4. Rebuild and reinstall TaskMate.")
                }
            } else if (vm.uid == null) {
                LoginScreen(vm)
            } else {
                key(vm.uid) { TasksScreen(vm) }
            }
        }
    }
}

@Composable
private fun LoginScreen(vm: TaskMateViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    // Password stays in memory and is not saved across Activity recreation.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var mismatch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(30.dp))
        Text("TaskMate", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("A little plan. A calmer study day.", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Text(if (register) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
            enabled = !vm.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it; mismatch = false }, label = { Text("Password") },
            singleLine = true, enabled = !vm.busy, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } })
        if (register) {
            OutlinedTextField(confirm, { confirm = it; mismatch = false }, label = { Text("Confirm password") },
                singleLine = true, enabled = !vm.busy, modifier = Modifier.fillMaxWidth(),
                isError = mismatch, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (mismatch) Text("Passwords do not match.", color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = {
            if (register && password != confirm) mismatch = true
            else vm.authenticate(email, password, register)
        }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (vm.busy) "Please wait…" else if (register) "Create account" else "Log in")
        }
        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick = { register = !register; confirm = ""; password = ""; mismatch = false },
            enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (register) "Already have an account? Log in" else "New here? Create an account")
        }
        if (!register) TextButton(onClick = { vm.resetPassword(email) }, enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth()) { Text("Forgot password?") }
    }
}

@Composable
private fun TasksScreen(vm: TaskMateViewModel) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = vm.tasks.firstOrNull { it.id == editingId }
    var deleting by remember { mutableStateOf<StudentTask?>(null) }
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("All") }
    val shown = vm.tasks.filter { when (filter) {
        "Active" -> !it.completed
        "Done" -> it.completed
        else -> true
    } }.sortedWith(compareBy<StudentTask> { it.completed }.thenBy { it.deadline.ifBlank { "9999-12-31" } })
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TaskMate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { profileOpen = true }) { Text("Profile") }
        }
        Text("${vm.tasks.count { !it.completed }} tasks left · ${vm.tasks.count { it.completed }} completed")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Active", "Done").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
            }
        }
        if (!vm.storageHealthy) Text("Saved tasks could not be loaded. Please restart the app.", color = MaterialTheme.colorScheme.error)
        if (shown.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (vm.tasks.isEmpty()) "Your list is clear. Add your first task!" else "No tasks in this filter.")
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)) {
                items(shown, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = task.completed, onCheckedChange = { vm.toggle(task) }, enabled = vm.storageHealthy)
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                                    if (task.subject.isNotBlank()) Text(task.subject, style = MaterialTheme.typography.bodySmall)
                                    if (task.deadline.isNotBlank()) Text("Due: ${task.deadline}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingId = task.id; editorOpen = true }, enabled = vm.storageHealthy) { Text("Edit") }
                                TextButton(onClick = { deleting = task }, enabled = vm.storageHealthy) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { editingId = null; editorOpen = true }, enabled = vm.storageHealthy,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text("+ Add task") }
    }
    if (editorOpen) TaskEditor(editing, vm.message, onDismiss = { editorOpen = false },
        onSave = { title, subject, deadline -> if (vm.saveTask(editing, title, subject, deadline)) editorOpen = false })
    deleting?.let { task ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete task?") },
            text = { Text("Delete “${task.title}”? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.delete(task); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
    if (profileOpen) AlertDialog(onDismissRequest = { profileOpen = false }, title = { Text("Your account") },
        text = { Text("${vm.email}\n\nTasks are saved on this device for your account. They do not sync to other devices.") },
        confirmButton = { TextButton(onClick = { profileOpen = false; vm.logout() }) { Text("Log out") } },
        dismissButton = { TextButton(onClick = { profileOpen = false }) { Text("Close") } })
}

@Composable
private fun TaskEditor(task: StudentTask?, error: String?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var subject by rememberSaveable(task?.id) { mutableStateOf(task?.subject.orEmpty()) }
    var deadline by rememberSaveable(task?.id) { mutableStateOf(task?.deadline.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (task == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title *") }, singleLine = true)
                OutlinedTextField(subject, { subject = it.take(80) }, label = { Text("Subject (optional)") }, singleLine = true)
                OutlinedTextField(deadline, { deadline = it.take(10) }, label = { Text("Deadline (optional)") },
                    placeholder = { Text("YYYY-MM-DD") }, singleLine = true)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, subject, deadline) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
