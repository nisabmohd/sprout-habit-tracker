package app.sprout.habits.ui

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** True when dates use the English patterns as written ("Monday, 28 September", "21 – 27 Sep"). */
fun englishDates(): Boolean = Locale.getDefault().language == "en"

/**
 * A formatter for [pattern] ("EEEE, d MMMM") in the app's language. English keeps the pattern
 * as written; other languages get the same fields in their own order and punctuation
 * ("Montag, 28. September", "9月28日月曜日"). Built on each call so a language change applies.
 */
fun datePattern(pattern: String): DateTimeFormatter {
    val locale = Locale.getDefault()
    if (englishDates()) return DateTimeFormatter.ofPattern(pattern, locale)
    val skeleton = pattern.filter { it.isLetter() }
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}
