package zoom.schedule.sync

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.text.SimpleDateFormat

fun parseFullSession(document: XWPFDocument, docxName: String): List<ScheduleEntry> {
    return document.tables.flatMap { table ->
        table.rows.drop(1).flatMap { row ->
            val day = row.tableCells.first().text.filter { c -> c.isDigit() || c == '.' }
            row.tableCells.drop(1).mapIndexedNotNull { index, cell ->
                if (isMe(cell.text)) {
                    val time = cell.text.substringAfterLast(',')
                        .trim().replace(".", ":")
                    val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
                    val formatted = "$day $time"
                    val date = format.parse(formatted)
                    ScheduleEntry(
                        start = date,
                        end = date,
                        groupNames = listOf(table.rows.first().tableCells[index].text.trim()),
                        subjectName = cell.text.trim(),
                        docxNames = listOf(docxName)
                    )
                } else {
                    null
                }
            }
        }
    }
}