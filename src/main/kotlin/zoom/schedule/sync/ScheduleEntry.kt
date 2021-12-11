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
    val docxName: String
) {
    // формат, в котором элементы расписания будут отображены в дибаге (при отладке)
    override fun toString(): String {
        return "$start $subjectName ${groupNames.joinToString(separator = " ")}"
    }
}