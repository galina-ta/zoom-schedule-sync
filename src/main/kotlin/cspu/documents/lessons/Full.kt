package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import java.text.SimpleDateFormat

// разобрать документ с расписанием очного отделения
fun parseFull(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуентов в плоский список элементов расписания
    return document.tables.flatMap { table ->
        // преобразовать список ячеек первой строки таблицы в список групп
        val groups = parseGroups(table)
        // разобрать рабочие дни и получить список пар
        parseLessons(table.rows, docxName, groups)
    }
}

// получаем список групп из таблицы
private fun parseGroups(table: XWPFTable): List<Group> {
    // преобразуем ячейки первой строки таблицы по следующему правилу
    return table.rows[0].tableCells.mapNotNull { cell ->
        // если текст ячейки является названием группы, то создать и добавить в список группу
        if (isGroupName(text = cell.text)) {
            // у которой имя это текст текущей ячейки без пробельных символов в начале и конце
            Group(
                name = cell.text.trim(),
                // ширина ячейки является шириной текущей ячейки
                cellWidth = cellWidth(cell)
            )
        } else {
            // иначе ничего не добавлять
            null
        }
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
                return CommonSubject(name = subjectName, cellWidth = cellWidth)
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
    val name: String,
    // ширина ячейки общей пары изначально равно 0 (не посчитано)
    val cellWidth: Int
)

//получение пар из строк таблицы
private fun parseLessons(
    rows: List<XWPFTableRow>,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    // текущая дата по умолчанию не задана
    var currentDay: String? = null
    // возращаем список строк, преобразованный в список пар по следующему правилу
    return rows.flatMap { row ->
        // пробуем получить дату из текущей строки
        val rowDay = row.tableCells[0].text.filter { char -> char.isDigit() || char == '.' }
        // если дата указана для этой строки (первая строка текущего дня)
        if (rowDay.isNotBlank()) {
            // то текущий день - это день текущей строки
            currentDay = rowDay
        }
        //если текущая дата уже найдена, то
        if (currentDay != null) {
            // получаем пары из одной строки
            parseLessonsWithSameTime(row, currentDay!!, docxName, groups)
        } else {
            // находимся на строке заголовка (не дошли до пар) поэтому не добавляем пары из этой строки
            emptyList()
        }
    }
}

// получение всех пар, которые идут одновременно
private fun parseLessonsWithSameTime(
    row: XWPFTableRow,
    currentDay: String,
    docxName: String,
    groups: List<Group>
): List<Lesson> {
    // стандартизируем черточки, заменяем в строке времени точки на двоеточие при разнообразии
    val time = standardizeDashes(text = row.tableCells[1].text).replace(".", ":")
    // берем время начала и убираем из него пробельные символы
    val startTime = time.substringBefore(dash).trim()
    // берем время конца и убираем из него пробельные символы
    val endTime = time.substringAfter(dash).trim()
    // получем дату и время начала события
    val formattedStart = "$currentDay $startTime"
    // получем дату и время конца события
    val formattedEnd = "$currentDay $endTime"
    // задаем формат представления даты и времени
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
    // устанавливаем "каретку" на ячейку с индеком 2
    var currentCellIndex = 2
    //находим общую пару у групп
    val commonSubject = findCommonSubject(row, groups)
    // если все ячейки строки, кроме первых трех и последней - пустые
    return if (row.tableCells.drop(2).dropLast(1).all { cell -> cell.text.isBlank() }) {
        // то не добавляем элементы расписания из этой строки
        emptyList()
    } else {
        // если потоковая пара определена
        if (commonSubject != null) {
            // если текст последней ячейки содержит мою фамилию и инициалы
            if (containsMyNameShort(text = commonSubject.name)) {
                // возвращаем список из одного элемента расписания,
                // чтобы добавился от этой строки в общий список текущего документа
                listOf(
                    Lesson(
                        // время начала этого элемента расписания разбираем по формату
                        start = format.parse(formattedStart),
                        // время конца этого элемента расписания разбираем по формату
                        end = format.parse(formattedEnd),
                        // список названий групп - это имена с индексом 0 и 1
                        groupNames = listOf(groups[0].name, groups[1].name),
                        // название дисциплины это название поточной дисциплины
                        subjectName = "${clearSubjectName(commonSubject.name)} ${row.tableCells.last().text.trim()}",
                        // название документа, который прикрепится к этому элементу расписания
                        // это название текущего документа
                        docxNames = listOf(docxName)
                    )
                )
            } else {
                // иначе не добавляем элементы расписания из этой строки
                emptyList()
            }
        } else {
            // если в строке хотя бы одна ячейка с моей фамилией иинициалами
            if (row.tableCells.any { cell -> containsMyNameShort(cell.text) }) {
                groups.flatMap { group ->
                    // список названий дисциплин - это изначально пустой изменяемый список
                    val subjectNames = mutableListOf<String>()
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
                        // название дисциплины это текст текущей ячейки
                        // без пробельных символов в начале и конце
                        val subjectName = currentCell.text.trim()
                        // если моя фамилия и инициалы содержится в названии дисциплины
                        if (containsMyNameShort(subjectName)) {
                            // в списке ячеек текущей строки, начиная с текущей позиции "каретки"
                            val roomName = row.tableCells.drop(currentCellIndex)
                                // ищем первую ячейку (если есть) с текстом, состоящим не только из пробельных символов
                                .firstOrNull { cell -> cell.text.isNotBlank() }
                                // если нашли, у текста ячейки убираем пробельные символы
                                ?.text?.trim()
                            // если аудитория нашлась
                            if (roomName != null) {
                                // добавляем в список дисциплин название дисциплины и аудиторию
                                subjectNames.add("${clearSubjectName(subjectName)} $roomName")
                            } else {
                                // иначе добавляем только очищенное название дисциплины
                                subjectNames.add(clearSubjectName(subjectName))
                            }
                        }
                    }
                    subjectNames.map { subjectName ->
                        // создаем элемент расписания
                        Lesson(
                            // время начала этого элемента расписания разбираем по формату
                            start = format.parse(formattedStart),
                            // время конца этого элемента расписания разбираем по формату
                            end = format.parse(formattedEnd),
                            // список названий групп - это список из названия текущей группы
                            groupNames = listOf(group.name),
                            // название дисциплины это название дисциплины
                            subjectName = subjectName,
                            // название документа, который прикрепится к этому элементу расписания
                            // это название текущего документа
                            docxNames = listOf(docxName)
                        )
                    }
                }
            } else {
                // иначе не добавляем элементы расписания из этой строки
                emptyList()
            }
        }
    }
}

// очистить название дисциплины
private fun clearSubjectName(subjectName: String): String {
    // возвращаем название дисциплины до моей фамилии, очищенное от
    return subjectName.substringBefore("Терехова").trim { char ->
        // пробелов, запятых и точек
        char.isWhitespace() || char == ',' || char == '.'
    }
}