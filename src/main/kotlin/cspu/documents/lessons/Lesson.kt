package cspu.documents.lessons

import java.util.*

// описание записи в календаре
class Lesson(
    // время проведения пары
    val time: Time,
    // список названий групп
    val groupNames: List<String>,
    // название дисциплины и аудитории (если есть)
    val subjectDescription: String,
    // название файла, из которого взяты занятия
    val docxNames: List<String>
) {
    // формат, в котором элементы расписания будут отображены в дебаге (при отладке)
    override fun toString(): String {
        return "${time.start} $subjectDescription ${groupNames.joinToString(separator = " ")}"
    }

    class Time(
        // начало занятия
        val start: Date,
        // конец занятия
        val end: Date
    )
}

// преобразуем расписание с дубликатами в расписание без дубликатов
fun deduplicate(lessons: List<Lesson>): List<Lesson> {
    // возвращаем расписание
    return lessons
        // элементы которого сгруппированы по времени начала
        .groupBy { entry -> entry.time.start }
        // и каждый элемент сгруппированной структуры преобразован в
        .values.map { entries ->
            // элемент расписания, у которого
            Lesson(
                time = Lesson.Time(
                    // время начала - это время начала перого элемента группы
                    start = entries.first().time.start,
                    //время окончания - это максимальное время окончания в группе
                    end = entries.maxOf { entry -> entry.time.end }
                ),
                // название группы - это названия всех групп всех элементов группы записей календаря
                groupNames = entries.flatMap { entry -> entry.groupNames },
                // название дисциплины и аудитории - это названия дисциплины и аудитории первого элемента записи
                subjectDescription = entries.first().subjectDescription,
                //названия файлов - это названия всех файлов, из которых взяты элементы группы записей календаря
                docxNames = entries.flatMap { entry -> entry.docxNames }
            )
        }
}