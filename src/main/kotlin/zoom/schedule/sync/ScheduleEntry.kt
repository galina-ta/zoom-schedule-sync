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

// преобразуем расписание с дубликатами в расписание без дубликатов
fun deduplicate(schedule: List<ScheduleEntry>): List<ScheduleEntry> {
    // возвращаем расписание
    return schedule
        // элементы которого сгруппированы по времени начала
        .groupBy { entry -> entry.start }
        // и каждый элемент сгруппированной структуры преобразован в
        .values.map { entries ->
            // элемент расписания, у которого
            ScheduleEntry(
                // время начала - это время начала перого элемента группы
                start = entries.first().start,
                //время окончания - это максимальное время окончания в группе
                end = entries.maxOf { entry -> entry.end },
                // название группы - это названия всех групп всех элементов группы записей календаря
                groupNames = entries.flatMap { entry -> entry.groupNames },
                // название дисциплины и аудитории - это названия дисциплины и аудитории первого элемента записи
                subjectName = entries.first().subjectName,
                //названия файлов - это названия всех файлов, из которых взяты элементы группы записей календаря
                docxNames = entries.flatMap { entry -> entry.docxNames }
            )
        }
}