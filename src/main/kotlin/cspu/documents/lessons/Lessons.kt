package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

fun loadLessons(usualDir: File, sessionDir: File): List<Lesson>{
    // преобразуем список файлов в плоский список элементов расписания (в общее расписание)
    val usualLessons = usualDir.listFiles()!!.flatMap { docxFile ->
        // если текущий файл не папка
        if (!docxFile.isDirectory) {
            // получаем документв формате библиотеки poi, передав ей возможность считать содержание файла
            val document = XWPFDocument(docxFile.inputStream())
            // получаем все ячейки первой строки первого документа
            val groupCells = document.tables[0].rows[0].tableCells
            // если текст хотя бы одной ячейки с названием группы является названием заочной группой,
            if (groupCells.any { cell -> isShortGroupName(text = cell.text) }) {
                // разбираем документ как расписание заочных групп и добавляем в общее расписание
                parseShort(document, docxName = docxFile.name)
            } else {
                // если текст хотя бы одной ячейки с названием группы является названием очной группой
                if (groupCells.any { cell -> isFullGroupName(text = cell.text) }) {
                    // разбираем документ как расписание очных групп и добавляем в общее расписание
                    parseFull(document, docxName = docxFile.name)
                } else {
                    // кидаем ошибку
                    throw RuntimeException("документ ${docxFile.name} не является ни очным, ни заочным расписанием")
                }
            }
        } else {
            // ничего не добавляем при работе с папкой
            emptyList()
        }
    }
    val sessionLessons = sessionDir.listFiles()!!.flatMap { docxFile ->
        val document = XWPFDocument(docxFile.inputStream())
        parseFullSession(document, docxName = docxFile.name)
    }
    // получение расписания без дублирования
    val deduplicatedUsual = deduplicate(usualLessons)
    return deduplicatedUsual + sessionLessons
}