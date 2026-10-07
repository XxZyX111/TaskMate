package com.aqeela.taskmate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StudentTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String = "",
    val deadline: String = "",
    val completed: Boolean = false
)

/** Only task data is stored here. Firebase manages credentials and sessions. */
class TaskStore(context: Context) {
    private val preferences = context.getSharedPreferences("student_tasks", Context.MODE_PRIVATE)

    fun load(uid: String): List<StudentTask> {
        val array = JSONArray(preferences.getString("tasks_$uid", "[]"))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            StudentTask(item.getString("id"), item.getString("title"),
                item.optString("subject"), item.optString("deadline"), item.optBoolean("completed"))
        }
    }

    fun save(uid: String, tasks: List<StudentTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().put("id", task.id).put("title", task.title)
                .put("subject", task.subject).put("deadline", task.deadline)
                .put("completed", task.completed))
        }
        // apply updates memory immediately and writes the small list asynchronously.
        preferences.edit().putString("tasks_$uid", array.toString()).apply()
    }
}
