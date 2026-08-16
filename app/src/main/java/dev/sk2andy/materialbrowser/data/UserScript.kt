package dev.sk2andy.materialbrowser.data

import org.json.JSONObject
import java.util.UUID

/**
 * Injection timing for the script in the page lifecycle.
 */
enum class UserScriptRunAt {
    DOCUMENT_START, // Injected when page starts loading (onPageStarted)
    DOCUMENT_END    // Injected when page DOM and resources finish loading (onPageFinished)
}

/**
 * Represents a custom UserScript with wildcard/domain matching capabilities.
 */
data class UserScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val urlPattern: String = "*",
    val code: String,
    val isEnabled: Boolean = true,
    val runAt: UserScriptRunAt = UserScriptRunAt.DOCUMENT_END
) {
    /**
     * Checks if this script should run on the given [url].
     * Supports wildcards like "*", "*://*.youtube.com/*", "example.com", or regex.
     */
    fun matchesUrl(url: String): Boolean {
        if (!isEnabled || url.isBlank()) return false
        val pattern = urlPattern.trim()
        if (pattern.isEmpty() || pattern == "*" || pattern == "<all_urls>" || pattern == "*://*/*") {
            return true
        }

        return try {
            val regexString = buildRegexFromPattern(pattern)
            val regex = Regex(regexString, RegexOption.IGNORE_CASE)
            regex.containsMatchIn(url)
        } catch (_: Exception) {
            // Fallback substring matching
            url.contains(pattern, ignoreCase = true)
        }
    }

    private fun buildRegexFromPattern(glob: String): String {
        if (glob.startsWith("^") || (glob.startsWith("/") && glob.endsWith("/"))) {
            return glob.trim('/')
        }
        val cleanGlob = if (!glob.contains("://") && !glob.startsWith("*")) {
            "*$glob*"
        } else {
            glob
        }
        val sb = StringBuilder("^")
        for (c in cleanGlob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append(".")
                '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> {
                    sb.append("\\").append(c)
                }
                else -> sb.append(c)
            }
        }
        sb.append("$")
        return sb.toString()
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("urlPattern", urlPattern)
            put("code", code)
            put("isEnabled", isEnabled)
            put("runAt", runAt.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): UserScript {
            val runAtParsed = try {
                UserScriptRunAt.valueOf(json.optString("runAt", UserScriptRunAt.DOCUMENT_END.name))
            } catch (_: Exception) {
                UserScriptRunAt.DOCUMENT_END
            }
            return UserScript(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Untitled Script"),
                urlPattern = json.optString("urlPattern", "*"),
                code = json.optString("code", ""),
                isEnabled = json.optBoolean("isEnabled", true),
                runAt = runAtParsed
            )
        }
    }
}
