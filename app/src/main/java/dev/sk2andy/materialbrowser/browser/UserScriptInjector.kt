package dev.sk2andy.materialbrowser.browser

import android.webkit.WebView
import android.util.Base64
import dev.sk2andy.materialbrowser.data.UserScript

/**
 * Handles the safe injection of UserScript JavaScript code into a WebView.
 */
object UserScriptInjector {

    /**
     * Injects a list of matched scripts into the target WebView.
     * We encode the script to Base64 first and decode it inside the WebView to avoid
     * quotation/escaping syntax errors when passing raw JS strings through evaluateJavascript.
     */
    fun injectScripts(webView: WebView?, scripts: List<UserScript>) {
        if (webView == null || scripts.isEmpty()) return

        val combinedScript = StringBuilder()
        
        // Wrap execution in an IIFE (Immediately Invoked Function Expression) to prevent global scope pollution
        combinedScript.append("(function() {")

        for (script in scripts) {
            val base64Code = Base64.encodeToString(script.code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            
            // Build safe executor
            combinedScript.append("""
                try {
                    var decodedScript = decodeURIComponent(escape(window.atob('${base64Code}')));
                    var scriptEl = document.createElement('script');
                    scriptEl.type = 'text/javascript';
                    scriptEl.text = decodedScript;
                    // Tag it for debugging if needed
                    scriptEl.setAttribute('data-userscript-name', '${script.name.replace("'", "\\'")}');
                    (document.head || document.documentElement).appendChild(scriptEl);
                    scriptEl.remove(); // Cleanup DOM after execution
                } catch(e) {
                    console.error('Error injecting UserScript [${script.name.replace("'", "\\'")}]: ', e);
                }
            """.trimIndent())
        }

        combinedScript.append("})();")

        // Run on UI thread as evaluateJavascript requires it
        webView.post {
            try {
                webView.evaluateJavascript(combinedScript.toString(), null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
