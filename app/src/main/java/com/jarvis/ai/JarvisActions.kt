package com.jarvis.ai

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import org.json.JSONArray

object JarvisActions {
    private const val PREFS = "jarvis_memory"
    private const val KEY = "items"

    fun execute(context: Context, prompt: String): String? {
        val text = prompt.trim()
        val lower = text.lowercase()
        val memoryPrefix = when {
            lower.startsWith("recuerda que ") -> "recuerda que "
            lower.startsWith("acuérdate de ") -> "acuérdate de "
            lower.startsWith("acuérdate que ") -> "acuérdate que "
            else -> null
        }
        if (memoryPrefix != null) {
            saveMemory(context, text.drop(memoryPrefix.length).trim())
            return "Hecho. Lo recordaré en este dispositivo."
        }
        if (lower.contains("qué recuerdas") || lower.contains("que recuerdas") || lower.contains("mis recuerdos")) {
            val memories = readMemories(context)
            return if (memories.isEmpty()) "Todavía no tengo recuerdos guardados." else "Recuerdo: " + memories.joinToString("; ")
        }
        if (lower.startsWith("busca en internet ") || lower.startsWith("busca en google ") || lower.startsWith("buscar en internet ")) {
            val query = text.substringAfter(' ').substringAfter(' ').trim()
            if (query.isNotBlank()) {
                openUrl(context, "https://www.google.com/search?q=" + Uri.encode(query))
                return "He abierto una búsqueda de Internet sobre $query."
            }
        }
        if (lower.startsWith("abre mapas") || lower.startsWith("abre google maps")) {
            openPackageOrUrl(context, "com.google.android.apps.maps", "https://maps.google.com")
            return "Abriendo mapas."
        }
        if (lower.startsWith("llévame a ") || lower.startsWith("llevame a ") || lower.startsWith("cómo llegar a ") || lower.startsWith("como llegar a ")) {
            val destination = text.substringAfter(" a ").trim()
            openUrl(context, "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination))
            return "He preparado la ruta a $destination."
        }
        if (lower.startsWith("abre whatsapp")) return launchApp(context, "com.whatsapp", "WhatsApp")
        if (lower.startsWith("abre youtube")) return launchApp(context, "com.google.android.youtube", "YouTube")
        if (lower.startsWith("abre spotify")) return launchApp(context, "com.spotify.music", "Spotify")
        if (lower.startsWith("abre chrome")) return launchApp(context, "com.android.chrome", "Chrome")
        if (lower.startsWith("abre alexa") || lower.contains("abre la aplicación alexa")) return launchApp(context, "com.amazon.dee.app", "Alexa")
        if (lower.startsWith("abre ajustes") || lower.startsWith("abre configuración") || lower.startsWith("abre configuracion")) {
            safeStart(context, Intent(Settings.ACTION_SETTINGS))
            return "Abriendo los ajustes del teléfono."
        }
        if (lower.startsWith("añade al calendario ") || lower.startsWith("anade al calendario ") || lower.startsWith("crear evento ")) {
            val title = text.substringAfter(' ').substringAfter(' ').trim()
            safeStart(context, Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, title))
            return "He abierto el calendario para crear el evento: $title."
        }
        if (lower.startsWith("llama al ") || lower.startsWith("llama a ")) {
            val number = text.substringAfter(' ').substringAfter(' ').trim().replace(" ", "")
            safeStart(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
            return "He abierto el marcador para $number."
        }
        if (lower.contains("controla mi casa") || lower.contains("casa inteligente")) return launchApp(context, "com.amazon.dee.app", "Alexa")
        return null
    }

    private fun saveMemory(context: Context, value: String) {
        if (value.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrElse { JSONArray() }
        array.put(value.take(300))
        while (array.length() > 20) array.remove(0)
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun readMemories(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrElse { JSONArray() }
        return buildList { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    private fun launchApp(context: Context, packageName: String, name: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            safeStart(context, intent)
            "Abriendo $name."
        } else "No encuentro $name instalado en este teléfono."
    }

    private fun openPackageOrUrl(context: Context, packageName: String, fallback: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) safeStart(context, intent) else openUrl(context, fallback)
    }

    private fun openUrl(context: Context, url: String) = safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    private fun safeStart(context: Context, intent: Intent) {
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }
}
