package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument

// разобрать документ с расписанием заочного отделения
fun parseShort(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуента в плоский список элементов расписания
    return document.tables.flatMap { table ->
        // преобразовать список ячеек первой строки таблицы в список групп
        val groups = parseGroups(table)
        // текущая дата изначально не задана
        var currentDay: String? = null
        // преобразовать список строк таблицы в плоский список элементов расписания
        table.rows.flatMap { row ->
            // пробуем получить дату из текущей строки
            val rowDay = row.tableCells[1].text.trim()
            // если дата указана для этой строки (первая строка текущего дня)
            if (rowDay.isNotBlank()) {
                // то текущий день - это день текущей строки
                currentDay = rowDay
            }
            // устанавливаем "каретку" на ячейку с индеком 3
            var currentCellIndex = 3

            // название дисциплины потоковой пары изначально не определено
            var commonSubjectName: String? = null
            // ширина ячейки общей пары изначально равно 0 (не посчитано)
            var commonSubjectCellWidth = 0
            // для каждой ячейки текущей строки, кроме первых трех
            for (cell in row.tableCells.drop(3)) {
                // название дисциплины - это текст текущей ячейки без пробелов в начале и конце
                val subjectName = cell.text.trim()
                // если название дисциплины не пустое
                if (subjectName.isNotEmpty()) {
                    // если название дисциплины уже найдено
                    if (commonSubjectName != null) {
                        // прерываем поиск поточной пары
                        break
                    } else {
                        // иначе название дисциплины общей пары - это название дисциплины
                        commonSubjectName = subjectName
                        // получаем ширину ячейки
                        val cellWidth = cellWidth(cell)
                        // ищем максимальную ширину ячейки в строке:
                        // если текущая ширина ячейки общей пары меньше ширины текущей ячейки
                        if (commonSubjectCellWidth < cellWidth) {
                            // устанавливаем, что ширина общей пары равна ширине текущей ячейки
                            commonSubjectCellWidth = cellWidth
                        }
                    }
                }
            }

            // если все ячейки строки, кроме первых трех и последней не пустые
            if (row.tableCells.drop(3).dropLast(1).all { cell -> cell.text.isBlank() }
                // или если все ячейки строки не содержат мою фамилию и инициалы
                || row.tableCells.all { cell -> !containsMyNameShort(cell.text) }) {
                // то не добавляем элементы расписания из этой строки
                emptyList()
            } else {
                // если количество групп равно двум и
                if (groups.size == 2 &&
                    // название потоковой дисциплины определено и
                    commonSubjectName != null &&
                    // ширина ячейки группы с индеком 0 меньше ширины ячейки потоковой дисциплины и
                    groups[0].cellWidth < commonSubjectCellWidth &&
                    // ширина ячейки группы с индеком 1 меньше ширины ячейки потоковой дисциплины и
                    groups[1].cellWidth < commonSubjectCellWidth
                ) {
                    // если текст последней ячейки содержит мою фамилию и инициалы
                    if (containsMyNameShort(text = row.tableCells.last().text)) {
                        // возвращаем список из одного элемента расписания,
                        // чтобы добавился от этой строки в общий список текущего документа
                        listOf(
                            Lesson(
                                time = parseLessonTime(cell = row.tableCells[2], currentDay!!),
                                // список названий групп - это имена с индексом 0 и 1
                                groupNames = listOf(groups[0].name, groups[1].name),
                                // название дисциплины это название поточной дисциплины
                                subjectDescription = commonSubjectName,
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
                    // иначе, если в строке нет потоковой пары, превращаем список групп
                    // в плоский список элементов расписания
                    groups.flatMap { group ->
                        // список названий дисциплин - это изначально пустой изменяемый список
                        val subjectNames = mutableListOf<String>()
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
                            val subjectName = currentCell.text.trim()
                            // если название дисциплины не состоит только из пробельных символов
                            if (subjectName.isNotBlank()) {
                                // добавить название дисциплины в список дисциплин текущей группы
                                subjectNames.add(subjectName)
                            }
                            // перевести "каретку" в следующую ячейку
                            currentCellIndex += 1
                        }

                        // необработанная ячейка с преподом - это текст ячейки, на которую указывает "каретка"
                        val teacherRaw = row.tableCells[currentCellIndex].text
                        // перевести "каретку" в следующую ячейку
                        currentCellIndex += 1
                        // если мое имя в ячейке с преподом
                        if (containsMyNameShort(text = teacherRaw)) {
                            // преобразуем список дисциплин в список элементов расписания
                            subjectNames.map { subjectName ->
                                // создаем элемент расписания
                                Lesson(
                                    time = parseLessonTime(cell = row.tableCells[2], currentDay!!),
                                    // список названий групп - это список из названия текущей группы
                                    groupNames = listOf(group.name),
                                    // название дисциплины это название дисциплины
                                    subjectDescription = subjectName,
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
                }
            }
        }
    }
}