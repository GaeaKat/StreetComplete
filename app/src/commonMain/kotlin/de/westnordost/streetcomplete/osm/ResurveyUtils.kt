package de.westnordost.streetcomplete.osm

import de.westnordost.streetcomplete.util.ktx.systemTimeNow
import de.westnordost.streetcomplete.util.ktx.toLocalDate
import kotlinx.datetime.LocalDate

/** Returns all the known keys used for recording the date at which the tag with the given key
 *  should be checked again. */
fun getLastCheckDateKeys(key: String): Sequence<String> = sequenceOf(
    "$key:check_date", "check_date:$key",
    "$key:lastcheck", "lastcheck:$key",
    "$key:last_checked", "last_checked:$key"
)

/** Known keys for check date (whole element) */
val LAST_CHECK_DATE_KEYS = listOf(
    "check_date",
    "lastcheck",
    "last_checked",
    "survey:date",
    "survey_date"
)

/** This date as a check date string, e.g. 2024-12-06 */
fun LocalDate.toCheckDateString(): String = this.toString()

/** Current timestamp as check date string, e.g. 2024-12-06 */
fun nowAsCheckDateString(): String = systemTimeNow().toLocalDate().toCheckDateString()

/** Try to parse this string as a check date. Returns `null` if it cannot be parsed as that. */
fun String.toCheckDate(): LocalDate? {
    val groups = OSM_CHECK_DATE_REGEX.matchEntire(this)?.groupValues ?: return null
    val year = groups[1].toIntOrNull() ?: return null
    val month = groups[2].toIntOrNull() ?: return null
    val day = groups[3].toIntOrNull() ?: 1

    return try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** adds or modifies the given tag. If the updated tag is the same as before, sets the check date
 *  tag to today instead. */
fun Tags.updateWithCheckDate(key: String, value: String) {
    val previousValue = get(key)
    set(key, value)
    /* if the value is changed, set the check date only if it has been set before. Behavior
     * before v32.0 was to delete the check date. However, this destroys data that was
     * previously collected by another surveyor - we don't want to destroy other people's data.
     */
    if (previousValue == value || hasCheckDateForKey(key) || hasCheckDate()) {
        updateCheckDateForKey(key)
    }
}

/** Set/update solely the check date to today for the given [key], this also removes other less
 *  preferred check date keys. To avoid ambiguities, this also updates (existence) check date, if
 *  if set. */
fun Tags.updateCheckDateForKey(key: String) {
    setCheckDateForKey(key, systemTimeNow().toLocalDate())
}

/** Set the check date for the given [key]. To avoid ambiguities, this also updates (existence)
 *  check date, if set. */
fun Tags.setCheckDateForKey(key: String, date: LocalDate) {
    removeCheckDatesForKey(key)
    set("$SURVEY_MARK_KEY:$key", date.toCheckDateString())
    if (hasCheckDate()) setCheckDate(date)
}

/** Return whether a check date is set for the given [key] */
fun Tags.hasCheckDateForKey(key: String): Boolean =
    getLastCheckDateKeys(key).any { get(it) != null }

/** Delete any check date keys for the given [key] */
fun Tags.removeCheckDatesForKey(key: String) {
    getLastCheckDateKeys(key).forEach { remove(it) }
}

/** Set/update solely the check date for the entire item to today, this also removes other less
 *  preferred check date keys for the entire item. */
fun Tags.updateCheckDate() {
    setCheckDate(systemTimeNow().toLocalDate())
}

/** Set the check date to the given [date] */
fun Tags.setCheckDate(date: LocalDate) {
    removeCheckDates()
    set(SURVEY_MARK_KEY, date.toCheckDateString())
}

/** Return whether any check dates are set */
fun Tags.hasCheckDate(): Boolean =
    LAST_CHECK_DATE_KEYS.any { get(it) != null }

/** Delete any check date for the entire item */
fun Tags.removeCheckDates() {
    LAST_CHECK_DATE_KEYS.forEach { remove(it) }
}

/** Date format of the tags used for recording the date at which the element or tag with the given
 *  key should be checked again. Accepted date formats: 2000-11-11 but also 2000-11 */
private val OSM_CHECK_DATE_REGEX = Regex("([0-9]{4})-([0-9]{2})(?:-([0-9]{2}))?")

/** OSM key used by StreetComplete to mark that something has been checked */
const val SURVEY_MARK_KEY = "check_date"
