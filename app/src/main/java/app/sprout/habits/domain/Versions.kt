package app.sprout.habits.domain

/**
 * Release versions: "v1.2.0" (a GitHub tag) or "1.0.0-github" (our versionName). Only the
 * numbers count; a leading "v" and anything after "-" are ignored.
 */
fun versionParts(version: String): List<Int> =
    version.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        .split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

/** True when [candidate] is a later release than [current] ("1.10.0" > "1.9.3"). */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = versionParts(candidate)
    val b = versionParts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** The first [max] bullet lines ("- ", "* ") of release notes, without the bullets and Markdown emphasis. */
fun releaseHighlights(body: String?, max: Int = 5): List<String> =
    body.orEmpty().lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("- ") || it.startsWith("* ") }
        .map { it.drop(2).replace("**", "").replace("`", "").trim() }
        .filter { it.isNotEmpty() }
        .take(max)
        .toList()
