package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow

// разобрать документ с расписанием заочного отделения
fun parseShort(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуента в плоский список элементов расписания
    return document.tables.flatMap { table ->
        // преобразовать список ячеек первой строки таблицы в список групп
        val groups = parseGroups(table)
        //получение пар из строк таблицы
        return@flatMap parseLessons(rows = table.rows, docxName, groups)
    }
}

// ищем потоковую пару
private fun findCommonSubject(row: XWPFTableRow, groups: List<Group>): CommonSubject? {
    // для каждой ячейки текущей строки, кроме первых трех
    row.tableCells.drop(3).forEach { cell ->
        // название дисциплины - это текст текущей ячейки без пробелов в начале и конце
        val subjectName = cell.text.trim()
        // если название дисциплины не пустое
        if (subjectName.isNotEmpty()) {
            // получаем ширину ячейки
            val subjectCellWidth = cellWidth(cell)
            // если количество групп равно двум и
            return if (groups.size == 2 &&
                // ширина ячейки группы с индеком 0 меньше ширины ячейки потоковой дисциплины и
                // ширина ячейки группы с индеком 1 меньше ширины ячейки потоковой дисциплины и
                groups[0].cellWidth < subjectCellWidth && groups[1].cellWidth < subjectCellWidth
            ) {
                // то создаем и возвращаем поточную дисциплину с этим именем и просчитанной шириной ячейки
                CommonSubject(name = subjectName)
            } else {
                null
            }
        }
    }
    // если ни одна пара не нашлась в строке, то возвращаем отсутствие потоковой пары
    return null
}

//получение пар из строк таблицы
private fun parseLessons(
    rows: List<XWPFTableRow>,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    // текущая дата изначально не задана
    var currentDate: String? = null
    // возвращаем преобразованный список строк таблицы в плоский список элементов расписания
    return rows.flatMap { row ->
        // пробуем получить дату из текущей строки
        val rowDay = row.tableCells[1].text.trim()
        // если дата указана для этой строки (первая строка текущего дня)
        if (rowDay.isNotBlank()) {
            // то текущий день - это день текущей строки
            currentDate = rowDay
        }
        parseLessonsWithSameTime(row, workDayDate = currentDate!!, docxName, groups)
    }
}

// получение всех пар, которые идут одновременно
private fun parseLessonsWithSameTime(
    row: XWPFTableRow,
    workDayDate: String,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    // ищем потоковую пару
    val commonSubject = findCommonSubject(row, groups)
    // определяем есть ли мои пары в строке
    return if (!hasMyLessons(row)) {
        // то не добавляем элементы расписания из этой строки
        emptyList()
    } else {
        // если поточная пара нашлась
        if (commonSubject != null) {
            //делаем список из моей потоковой пары, если она есть
            listOfNotNull(
                parseMyCommonLesson(row, workDayDate, docxName, groups, commonSubject)
            )
        } else {
            //разбираем список пар отдельных групп
            parseMyGroupLessons(groups, row, workDayDate, docxName)
        }
    }
}

// есть ли  мои пары в строке
private fun hasMyLessons(row: XWPFTableRow): Boolean {
    // отбрасываем у строки первые две и последнюю ячейки
    // и проверяем является ли хотя бы одна из оставшихся не пустой
    return row.tableCells.drop(3).dropLast(1).any { cell -> cell.text.isNotBlank() }
            // и если хотя бы одна содержит мою фамилию и инициалы
            && row.tableCells.any { cell -> containsMyNameShort(cell.text) }
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
    return if (containsMyNameShort(text = row.tableCells.last().text)) {
        // возвращаем список из одного элемента расписания,
        // чтобы добавился от этой строки в общий список текущего документа
        Lesson(
            time = parseLessonTime(cell = row.tableCells[2], workDayDate),
            // список названий групп - это имена с индексом 0 и 1
            groupNames = listOf(groups[0].name, groups[1].name),
            // название дисциплины это название поточной дисциплины
            subjectDescription = commonSubject.name,
            // название документа, который прикрепится к этому элементу расписания
            // это название текущего документа
            docxNames = listOf(docxName)
        )
    } else {
        // иначе возвращаем отсутствие пары
        null
    }
}

// разбираем список пар отдельных групп
private fun parseMyGroupLessons(
    groups: List<Group>,
    row: XWPFTableRow,
    workDayDate: String,
    docxName: String
): List<Lesson> {
    // устанавливаем "каретку" на ячейку с индеком 3
    var currentCellIndex = 3
    // возвращаем список групп, превращенный в плоский список элементов расписания
    return groups.flatMap { group ->
        // список названий дисциплин - это изначально пустой изменяемый список
        val subjectDescriptions = mutableListOf<String>()
        // изначально ширина ячеек дисциплин текущей группы равна 0
        var currentSubjectCellsWidth = 0
        // пока эта ширина меньше ширины ячейки текущей группы
        while (currentSubjectCellsWidth < group.cellWidth) {
            // текущая ячейка - это та ячейка, на которую указывает "каретка"
            val currentCell = row.tableCells[currentCellIndex]
            // добавить к ширине ячеек групп ширину текущей ячейки
            currentSubjectCellsWidth += cellWidth(cell = currentCell)
            // название дисциплины это текст текущей ячейки
            // без пробельных символов в начале и конце
            val subjectDescription = currentCell.text.trim()
            // если название дисциплины не состоит только из пробельных символов
            if (subjectDescription.isNotBlank()) {
                // добавить название дисциплины в список дисциплин текущей группы
                subjectDescriptions.add(subjectDescription)
            }
            // перевести "каретку" в следующую ячейку
            currentCellIndex += 1
        }

        // необработанная ячейка с преподом - это текст ячейки, на которую указывает "каретка"
        val teacherCell = row.tableCells[currentCellIndex]
        // перевести "каретку" в следующую ячейку
        currentCellIndex += 1
        //описание дисциплины преобразуем в пары
        subjectDescriptionsToLessons(
            group,
            subjectDescriptions,
            row,
            docxName,
            workDayDate,
            teacherCell
        )
    }
}

// преобразуем описание дисциплин в пары
private fun subjectDescriptionsToLessons(
    group: Group,
    subjectDescriptions: List<String>,
    row: XWPFTableRow,
    docxName: String,
    workDayDate: String,
    teacherCell: XWPFTableCell
): List<Lesson> {
    // возвращаем результат преобразования по правилу
    // если мое имя в ячейке с преподом
    return if (containsMyNameShort(text = teacherCell.text)) {
        // преобразуем список дисциплин в список пар
        subjectDescriptions.map { subjectDescription ->
            // создаем пару
            Lesson(
                time = parseLessonTime(cell = row.tableCells[2], workDayDate),
                // список названий групп - это список из названия текущей группы
                groupNames = listOf(group.name),
                // название дисциплины это название дисциплины
                subjectDescription = subjectDescription,
                // название документа, который прикрепится к этому элементу расписания
                // это название текущего документа
                docxNames = listOf(docxName)
            )
        }
    } else {
        // иначе не добавляем элементы расписания из этой строки
        emptyList()
    }
}