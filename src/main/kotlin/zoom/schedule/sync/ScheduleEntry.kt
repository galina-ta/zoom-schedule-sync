package zoom.schedule.sync

import java.util.*

class ScheduleEntry(
    val start: Date,
    val end: Date,
    val groupNames: List<String>,
    val subjectName: String,
    val docxName: String
) {
    override fun toString(): String {
        return "$start $subjectName ${groupNames.joinToString(separator = " ")}"
    }
}