package com.daojia.app.data.api

/**
 * Cookie Parser - normalizes various cookie input formats to "k1=v1; k2=v2"
 * Supported formats:
 *   1. standard "key=value; key=value"
 *   2. JSON object: {"cookie":"..."} or {"k":"v",...}
 *   3. JSON array:  [{"name":"k","value":"v"}]
 *   4. document.cookie = "..."
 *   5. Netscape cookie.txt (lines with TAB columns, comments start with #)
 */
object CookieParser {

    fun parse(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val raw = input.trim()

        if (raw.startsWith("#") || (raw.contains("\t") && raw.contains("\n"))) {
            return parseNetscape(raw)
        }
        if (raw.startsWith("{")) {
            parseJsonObject(raw)?.let { return it }
        }
        val arrMatch = Regex("\\[[\\s\\S]*?]").find(raw)
        if (arrMatch != null) {
            parseJsonArray(arrMatch.value)?.let { return it }
        }
        Regex("document\\.cookie\\s*=\\s*[\"']([^\"']+)[\"']").find(raw)?.let {
            return it.groupValues[1]
        }
        return raw
    }

    private fun parseNetscape(raw: String): String {
        val out = mutableListOf<String>()
        for (lineRaw in raw.split('\n')) {
            val line = lineRaw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val cols = line.split('\t')
            if (cols.size >= 7) {
                val name = cols[5].trim()
                val value = cols[6].trim()
                if (name.isNotEmpty() && value.isNotEmpty() && !name.matches(Regex("^\\d+$"))) {
                    out += "$name=$value"
                }
            } else if (cols.size >= 2) {
                out += "${cols[0].trim()}=${cols[1].trim()}"
            }
        }
        return out.joinToString("; ")
    }

    private fun parseJsonObject(raw: String): String? {
        return try {
            Regex("\"cookie\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.let {
                return it.groupValues[1]
            }
            val items = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"")
                .findAll(raw)
                .map { "${it.groupValues[1]}=${it.groupValues[2]}" }
                .toList()
            if (items.isEmpty()) null else items.joinToString("; ")
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonArray(raw: String): String? {
        return try {
            val items = Regex("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"value\"\\s*:\\s*\"([^\"]*)\"[^}]*}")
                .findAll(raw)
                .map { "${it.groupValues[1]}=${it.groupValues[2]}" }
                .toList()
            if (items.isEmpty()) null else items.joinToString("; ")
        } catch (_: Exception) {
            null
        }
    }
}

