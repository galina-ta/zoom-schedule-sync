package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.text.SimpleDateFormat

// разобрать документ с расписанием сессии очного отделения
fun parseFullSession(document: XWPFDocument, docxName: String): List<Lesson> {
    // преобразовать список таблиц докуентов в плоский список элементов расписания
    return document.tables.flatMap { table ->
        table.rows.drop(1).flatMap { row ->
            val day = row.tableCells.first().text.filter { c -> c.isDigit() || c == '.' }
            row.tableCells.drop(1).mapIndexedNotNull { index, cell ->
                if (containsMyNameShort(cell.text)) {
                    val time = cell.text.substringAfterLast(',')
                        .trim().replace(".", ":")
                    val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
                    val formatted = "$day $time"
                    val date = format.parse(formatted)
                    Lesson(
                        time = Lesson.Time(
                            start = date,
                            end = date
                        ),
                        groupNames = listOf(table.rows.first().tableCells[index].text.trim()),
                        subjectDescription = cell.text.trim(),
                        docxNames = listOf(docxName)
                    )
                } else {
                    null
                }
            }
        }
    }
}