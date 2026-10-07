package com.aqeela.taskmate

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

class TaskMateViewModel(application: Application) : AndroidViewModel(application) {
    val configured = FirebaseApp.getApps(application).isNotEmpty()
    private val auth = if (configured) FirebaseAuth.getInstance() else null
    private val store = TaskStore(application)
    var uid by mutableStateOf<String?>(null); private set
    var email by mutableStateOf(""); private set
    var tasks by mutableStateOf(emptyList<StudentTask>()); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var storageHealthy by mutableStateOf(true); private set
    private val listener = FirebaseAuth.AuthStateListener { firebase ->
        uid = firebase.currentUser?.uid
        email = firebase.currentUser?.email.orEmpty()
        tasks = emptyList()
        storageHealthy = true
        uid?.let { userId ->
            try { tasks = store.load(userId) }
            catch (_: Exception) {
                storageHealthy = false
                message = "Saved tasks could not be loaded. Editing is disabled to protect your data."
            }
        }
    }
    init { auth?.addAuthStateListener(listener) }

    fun dismissMessage() { message = null }

    fun authenticate(inputEmail: String, password: String, register: Boolean) {
        if (busy || auth == null) return
        val address = inputEmail.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
            message = "Enter a valid email address."; return
        }
        if (password.isEmpty() || (register && password.length < 6)) {
            message = if (register) "Use a password with at least 6 characters." else "Enter your password."
            return
        }
        busy = true
        val request = if (register) auth.createUserWithEmailAndPassword(address, password)
            else auth.signInWithEmailAndPassword(address, password)
        request.addOnCompleteListener {
            busy = false
            if (!it.isSuccessful) message = if (register)
                "Could not create your account. Check your connection, password policy, or try signing in."
                else "Login failed. Check your email, password, and internet connection."
        }
    }

    fun resetPassword(inputEmail: String) {
        if (busy || auth == null) return
        val address = inputEmail.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
            message = "Enter your email first, then tap Forgot password."; return
        }
        busy = true
        auth.sendPasswordResetEmail(address).addOnCompleteListener {
            busy = false
            message = if (it.isSuccessful) "If this account exists, check your inbox for a reset link."
                else "Could not send a reset email. Check your connection and try again."
        }
    }

    fun saveTask(existing: StudentTask?, title: String, subject: String, deadline: String): Boolean {
        if (title.isBlank()) { message = "Give your task a title."; return false }
        if (deadline.isNotBlank()) {
            try {
                val parsed = LocalDate.parse(deadline.trim())
                require(parsed.toString() == deadline.trim())
            } catch (_: Exception) {
                message = "Use a valid deadline such as 2026-10-20, or leave it empty."; return false
            }
        }
        val item = StudentTask(existing?.id ?: java.util.UUID.randomUUID().toString(),
            title.trim(), subject.trim(), deadline.trim(), existing?.completed ?: false)
        return persist(if (existing == null) tasks + item else tasks.map { if (it.id == existing.id) item else it })
    }
    fun toggle(task: StudentTask) { persist(tasks.map { if (it.id == task.id) it.copy(completed = !it.completed) else it }) }
    fun delete(task: StudentTask) { persist(tasks.filterNot { it.id == task.id }) }
    private fun persist(updated: List<StudentTask>): Boolean {
        val userId = uid ?: return false
        if (!storageHealthy) return false
        return try { store.save(userId, updated); tasks = updated; true }
        catch (_: Exception) { message = "Could not save tasks. Please try again."; false }
    }
    fun logout() { message = null; auth?.signOut() }
    override fun onCleared() { auth?.removeAuthStateListener(listener); super.onCleared() }
}
