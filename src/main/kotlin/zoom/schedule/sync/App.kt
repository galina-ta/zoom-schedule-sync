package zoom.schedule.sync

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.lang.RuntimeException

fun main() {
    val dir = File("C:\\Users\\1255865\\Documents\\кафедра\\Расписание")
    val schedule = dir.listFiles().flatMap { docxFile ->
        if (docxFile.isFile) {
            val document = XWPFDocument(docxFile.inputStream())
            val groupCells = document.tables[0].rows[0].tableCells
            if (groupCells.any { cell -> isShortGroupName(text = cell.text) }) {
                parseShort(document, docxName = docxFile.name)
            } else {
                if (groupCells.any { cell -> isFullGroupName(text = cell.text) }) {
                    parseFull(document, docxName = docxFile.name)
                } else {
                    throw RuntimeException("документ ${docxFile.name} не является ни очным, ни заочным расписанием")
                }
            }
        } else {
            emptyList()
        }
    }
    exportInGoogleCalendar(schedule)
}