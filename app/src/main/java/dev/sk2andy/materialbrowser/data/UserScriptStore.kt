package dev.sk2andy.materialbrowser.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists UserScripts using SharedPreferences and exposes them as a Flow for the UI.
 */
class UserScriptStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("userscripts_prefs", Context.MODE_PRIVATE)
    }

    private val _scriptsFlow = MutableStateFlow<List<UserScript>>(emptyList())
    val scriptsFlow: StateFlow<List<UserScript>> = _scriptsFlow.asStateFlow()

    init {
        loadScripts()
    }

    private fun loadScripts() {
        val scriptsJsonString = prefs.getString("saved_scripts", "[]") ?: "[]"
        val loadedList = mutableListOf<UserScript>()
        try {
            val jsonArray = JSONArray(scriptsJsonString)
            for (i in 0 until jsonArray.length()) {
                val scriptJson = jsonArray.getJSONObject(i)
                loadedList.add(UserScript.fromJson(scriptJson))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _scriptsFlow.value = loadedList
    }

    private fun saveScripts(scripts: List<UserScript>) {
        val jsonArray = JSONArray()
        scripts.forEach { script ->
            jsonArray.put(script.toJson())
        }
        prefs.edit().putString("saved_scripts", jsonArray.toString()).apply()
        _scriptsFlow.value = scripts
    }

    fun addOrUpdateScript(script: UserScript) {
        val currentList = _scriptsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == script.id }
        if (index != -1) {
            currentList[index] = script
        } else {
            currentList.add(script)
        }
        saveScripts(currentList)
    }

    fun deleteScript(scriptId: String) {
        val currentList = _scriptsFlow.value.toMutableList()
        currentList.removeAll { it.id == scriptId }
        saveScripts(currentList)
    }

    fun toggleScript(scriptId: String, isEnabled: Boolean) {
        val currentList = _scriptsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == scriptId }
        if (index != -1) {
            val oldScript = currentList[index]
            currentList[index] = oldScript.copy(isEnabled = isEnabled)
            saveScripts(currentList)
        }
    }

    /**
     * Helper to get scripts directly without observing Flow (useful for WebView Controller)
     */
    fun getActiveScriptsForUrl(url: String?, runAt: UserScriptRunAt): List<UserScript> {
        if (url == null) return emptyList()
        return _scriptsFlow.value.filter { it.runAt == runAt && it.matchesUrl(url) }
    }
}
