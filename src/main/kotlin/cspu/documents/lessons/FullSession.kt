package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import java.text.SimpleDateFormat

// разобрать документ с расписанием сессии очного отделения
fun parseFullSession(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуентов в плоский список элементов расписания
    return document.tables.flatMap { table ->
        table.rows.drop(1).flatMap { row ->
            val day = parseDate(row)
            row.tableCells.drop(1).mapIndexedNotNull { index, cell ->
                parseMyLesson(cell, table, docxName, day, index)
            }
        }
    }
}

//получение даты из строки
private fun parseDate(row: XWPFTableRow): String {
    return row.tableCells.first().text.filter { c -> c.isDigit() || c == '.' }
}

//получение времени занятия (экзамена)
private fun parseSessionLessonTime(cell: XWPFTableCell, day: String): Lesson.Time {
    val time = parseTime(cell)
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
    val formatted = "$day $time"
    val date = format.parse(formatted)
    return Lesson.Time(
        start = date,
        end = date
    )
}

//получние времени
private fun parseTime(cell: XWPFTableCell): String {
    return cell.text.substringAfterLast(',').trim().replace(".", ":")
}

//получение моих пар
private fun parseMyLesson(
    cell: XWPFTableCell,
    table: XWPFTable,
    docxName: String,
    day: String,
    index: Int,
): Lesson? {
   return if (containsMyNameShort(cell.text)) {
        Lesson(
            //время занятия
            time = parseSessionLessonTime(cell, day),
            // список названий групп
            groupNames = listOf(table.rows.first().tableCells[index].text.trim()),
            //название дисциплины
            subjectDescription = cell.text.trim(),
            docxNames = listOf(docxName)
        )
    } else {
        null
    }
}