package zoom.schedule.sync

import java.util.*

// описание записи в календаре
class ScheduleEntry(
    // начало занятия
    val start: Date,
    // конец занятия
    val end: Date,
    // список названий групп
    val groupNames: List<String>,
    // название дисциплины и аудитории (если есть)
    val subjectName: String,
    // название файла, из которого взяты занятия
    val docxNames: List<String>
) {
    // формат, в котором элементы расписания будут отображены в дибаге (при отладке)
    override fun toString(): String {
        return "$start $subjectName ${groupNames.joinToString(separator = " ")}"
    }
}

fun deduplicate(schedule: List<ScheduleEntry>): List<ScheduleEntry> {
    return schedule
        .groupBy { entry -> entry.start }
        .values.map { entries ->
            ScheduleEntry(
                start = entries.first().start,
                end = entries.maxOf { entry -> entry.end },
                groupNames = entries.flatMap { entry -> entry.groupNames },
                subjectName = entries.first().subjectName,
                docxNames = entries.flatMap { entry -> entry.docxNames }
            )
        }
}