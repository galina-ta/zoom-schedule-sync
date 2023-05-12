package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import java.text.SimpleDateFormat

// разобрать документ с расписанием очного отделения
fun parseFull(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуентов в плоский список элементов расписания
    return document.tables.flatMap { table ->
        // преобразовать список ячеек первой строки таблицы в список групп
        val groups = parseGroups(table)
        // получить список пар
        parseLessons(table.rows, docxName, groups)
    }
}

// ищем поточную пару в строке таблицы
private fun findCommonSubject(row: XWPFTableRow, groups: List<Group>): CommonSubject? {
    // для каждой ячейки текущей строки, кроме первых двух
    for (cell in row.tableCells.drop(2)) {
        // название дисциплины - это текст текущей ячейки без пробелов в начале и конце
        val subjectName = cell.text.trim()
        // если название дисциплины не пустое
        if (subjectName.isNotEmpty()) {
            // получаем ширину ячейки
            val cellWidth = cellWidth(cell)
            // если количество групп равно двум и
            if (groups.size == 2 &&
                // ширина ячейки группы с индеком 0 меньше ширины ячейки потоковой дисциплины и
                // ширина ячейки группы с индеком 1 меньше ширины ячейки потоковой дисциплины и
                groups[0].cellWidth < cellWidth && groups[1].cellWidth < cellWidth
            ) {
                // то создаем и возвращаем поточную дисциплину с этим именем и просчитанной шириной ячейки
                return CommonSubject(name = subjectName)
            } else {
                return null
            }
        }
    }
    // иначе поточная дисцплина отсутсвует
    return null
}

private class CommonSubject(
    // название дисциплины потоковой пары изначально не определено
    val name: String
)

//получение пар из строк таблицы
private fun parseLessons(
    rows: List<XWPFTableRow>,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    // текущая дата по умолчанию не задана
    var currentDate: String? = null
    // возращаем список строк, преобразованный в список пар по следующему правилу
    return rows.flatMap { row ->
        // пробуем получить дату из текущей строки
        val rowDate = parseRowDate(row)
        // если дата указана для этой строки (первая строка текущего дня)
        if (rowDate.isNotBlank()) {
            // то текущий день - это день текущей строки
            currentDate = rowDate
        }
        //если текущая дата уже найдена, то
        if (currentDate != null) {
            // получаем пары из одной строки
            parseLessonsWithSameTime(row, workDayDate = currentDate!!, docxName, groups)
        } else {
            // находимся на строке заголовка (не дошли до пар) поэтому не добавляем пары из этой строки
            emptyList()
        }
    }
}

// пробуем получить дату из текущей строки
private fun parseRowDate(row: XWPFTableRow): String {
    return row.tableCells[0].text.filter { char -> char.isDigit() || char == '.' }
}

// получение всех пар, которые идут одновременно
private fun parseLessonsWithSameTime(
    row: XWPFTableRow,
    workDayDate: String,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    //находим общую пару у групп
    val commonSubject = findCommonSubject(row, groups)
    // если в строке нет пар
    return if (!hasLessons(row)) {
        // то не добавляем элементы расписания из этой строки
        emptyList()
    } else {
        // если потоковая пара определена
        if (commonSubject != null) {
            //делаем список из моей потоковой пары, если она есть
            listOfNotNull(
                // пробуем сформировать мою поточную пару
                parseMyCommonLesson(row, workDayDate, docxName, groups, commonSubject)
            )
        } else {
            //иначе получаем список отдельных групп
            parseMyGroupLessons(row, groups, workDayDate, docxName)
        }
    }
}

// есть ли пары в строке
private fun hasLessons(row: XWPFTableRow): Boolean {
    // отбрасываем у строки первые две и последнюю ячейки
    // и проверяем является ли хотя бы одна из оставшихся не пустой
    return row.tableCells.drop(2).dropLast(1).any { cell -> cell.text.isNotBlank() }
}

//получаем мои пары отдельных групп из строки
private fun parseMyGroupLessons(
    row: XWPFTableRow,
    groups: List<Group>,
    workDayDate: String,
    docxName: String
): List<Lesson> {
    // если в строке хотя бы одна ячейка с моей фамилией иинициалами
    return if (row.tableCells.any { cell -> containsMyNameShort(cell.text) }) {
        // создаем предоставитель пар отдельных групп и получаем список пар
        GroupLessonsProvider(row, workDayDate, docxName, groups).provide()
    } else {
        // иначе не добавляем элементы расписания из этой строки
        emptyList()
    }
}

//получает список пар отдельных групп из строки
private class GroupLessonsProvider(
    private val row: XWPFTableRow,
    private val workDayDate: String,
    private val docxName: String,
    private val groups: List<Group>
) {
    // устанавливаем "каретку" на ячейку с индеком 2
    private var currentCellIndex = 2

    // получаем список пар
    fun provide(): List<Lesson> {
        //для каждой группы
        return groups.flatMap { group ->
            // список названий моих дисциплин - это изначально пустой изменяемый список
            val mySubjectDescriptions = mutableListOf<String>()
            // изначально ширина ячеек дисциплин текущей группы равна 0
            var currentSubjectCellsWidth = 0
            // пока эта ширина меньше ширины ячейки текущей группы
            while (currentSubjectCellsWidth < group.cellWidth) {
                // текущая ячейка - это та ячейка, на которую указывает "каретка"
                val currentCell = row.tableCells.getOrNull(currentCellIndex)
                //если текущая ячейка отстутствует
                if (currentCell == null) {
                    // прекращаем работу с текущей парой текущей группы
                    break
                }
                // добавить к ширине ячеек групп ширину текущей ячейки
                currentSubjectCellsWidth += cellWidth(cell = currentCell)
                // перевести "каретку" в следующую ячейку
                currentCellIndex += 1
                // название моей дисциплины это текст текущей ячейки
                val mySubjectDescription = formatMySubjectDescription(currentCell)
                // если описание моей дисциплины нашлось
                if (mySubjectDescription != null) {
                    //добавляем описание моей дисциплины в список описания моих дисциплин
                    mySubjectDescriptions.add(mySubjectDescription)
                }
            }
            //преобразуем описание дисциплин в пары
            subjectDescriptionToLesson(group, mySubjectDescriptions)
        }
    }

    // формируем описание моей дисциплины
    private fun formatMySubjectDescription(currentCell: XWPFTableCell): String? {
        // название дисциплины - это текст текущей ячейки
        // без пробельных символов в начале и конце
        val cellText = currentCell.text.trim()
        // если моя фамилия и инициалы содержится в названии дисциплины
        if (containsMyNameShort(cellText)) {
            // ищем аудиторию
            val roomName = findRoomName()
            // если аудитория нашлась
            if (roomName != null) {
                // добавляем в список дисциплин название дисциплины и аудиторию
                return "${clearSubjectName(cellText)} $roomName"
            } else {
                // иначе добавляем только очищенное название дисциплины
                return clearSubjectName(cellText)
            }
        } else {
            // иначе в этой ячейке нет моей дисциплины
            return null
        }
    }

// находим аудиторию
    private fun findRoomName(): String? {
        // ищем в списке ячеек текущей строки, начиная с текущей позиции "каретки"
        return row.tableCells.drop(currentCellIndex)
            // ищем первую ячейку (если есть) с текстом, состоящим не только из пробельных символов
            .firstOrNull { cell -> cell.text.isNotBlank() }
            // если нашли, у текста ячейки убираем пробельные символы
            ?.text?.trim()
    }

    // создаем описание дисциплины
    private fun subjectDescriptionToLesson(
        group: Group,
        mySubjectDescriptions: List<String>
    ): List<Lesson> {
        // возвращаем результат преобразования по правилу
        return mySubjectDescriptions.map { subjectDescription ->
            // создаем пару
            Lesson(
                // время пары
                time = parseLessonTime(row, workDayDate),
                // список названий групп - это список из названия текущей группы
                groupNames = listOf(group.name),
                // название дисциплины это название дисциплины
                subjectDescription = subjectDescription,
                // название документа, который прикрепится к этому элементу расписания
                // это название текущего документа
                docxNames = listOf(docxName)
            )
        }
    }
}

//получаем мою поточную пару из строки
private fun parseMyCommonLesson(
    row: XWPFTableRow,
    workDayDate: String,
    docxName: String,
    groups: List<Group>,
    commonSubject: CommonSubject
): Lesson? {
    // если текст последней ячейки содержит мою фамилию и инициалы
    return if (containsMyNameShort(text = commonSubject.name)) {
        // возвращаем пару из текущей строки
        Lesson(
            // время пары
            time = parseLessonTime(row, workDayDate),
            // список названий групп - это имена с индексом 0 и 1
            groupNames = listOf(groups[0].name, groups[1].name),
            // название дисциплины это название поточной дисциплины
            subjectDescription = "${clearSubjectName(commonSubject.name)} ${row.tableCells.last().text.trim()}",
            // название документа, который прикрепится к этому элементу расписания
            // это название текущего документа
            docxNames = listOf(docxName)
        )
    } else {
        // иначе возвращаем отсутствие пары
        null
    }
}

//получаем время пары
private fun parseLessonTime(row: XWPFTableRow, workDayDate: String): Lesson.Time {
    // стандартизируем черточки, заменяем в строке времени точки на двоеточие при разнообразии
    val time = standardizeDashes(text = row.tableCells[1].text).replace(".", ":")
    // берем время начала и убираем из него пробельные символы
    val startTime = time.substringBefore(dash).trim()
    // берем время конца и убираем из него пробельные символы
    val endTime = time.substringAfter(dash).trim()
    // получем дату и время начала события
    val formattedStart = "$workDayDate $startTime"
    // получем дату и время конца события
    val formattedEnd = "$workDayDate $endTime"
    // задаем формат представления даты и времени
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm")

    return Lesson.Time(
        // время начала этого элемента расписания разбираем по формату
        start = format.parse(formattedStart),
        // время конца этого элемента расписания разбираем по формату
        end = format.parse(formattedEnd)
    )
}

// очистить название дисциплины
private fun clearSubjectName(subjectName: String): String {
    // возвращаем название дисциплины до моей фамилии, очищенное от
    return subjectName.substringBefore("Терехова").trim { char ->
        // пробелов, запятых и точек
        char.isWhitespace() || char == ',' || char == '.'
    }
}