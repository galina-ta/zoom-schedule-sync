package cspu.documents.practice

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

// объявить класс РПП с переменными в конструкторе
class Rpp(
    val docxName: String,
    val taskTypes: List<String>,
    val skills: List<Skill>
) {
    class Skill(
        val name: String,
        val z: String,
        val u: String,
        val v: String
    )
}

// разобрать документ РПП
fun loadRpp(docxFile: File): Rpp {
    //получить документ из файла
    val document = XWPFDocument(docxFile.inputStream())
    // таблица отчета это таблица документа, которую мы ищем по следующему признаку
    val reportTable = document.tables.find { table ->
        // в строке с индексом 0, если есть
        table.rows.getOrNull(0)
            // в тексте первой ячейки, приведенному к нижнему регистру, если есть
            ?.tableCells?.getOrNull(2)?.text?.lowercase()
            // если текст содержит последовательность знаков "Оценка результатов практики"
            ?.contains("оценка результатов практики") == true
    }!!
    // создание объекта РПП
    return Rpp(
        // в качестве записи перечня заданий практики передать строки без первых двух и без последних двух записей
        taskTypes = reportTable.rows.drop(2).dropLast(2).flatMap { row ->
            // берем текст ячейки с индексом 2 и разделяем по началу большой буквы
            row.tableCells[2].text.trim()
                .flatMap { c ->
                    if (c in 'А'..'Я') {
                        listOf('\n', c)
                    } else {
                        listOf(c)
                    }
                }
                .joinToString(separator = "")
                .split('\n')
                .map { taskType ->
                    taskType.trim().trim { c ->
                        c == ',' || c == '.' || c == '-' || c == '–'
                    }.trim().capitalize()
                }
                .filter { taskType ->
                    taskType.isNotBlank()
                }
        }.distinct(),
        //в качестве названия документа РПП передать название текущего документа
        docxName = docxFile.name,
        skills = reportTable.rows.drop(2).dropLast(2).chunked(3).map { chunk ->
            Rpp.Skill(
                name = chunk[0].tableCells[0].text.trim(),
                z = chunk[0].tableCells[2].text.trim(),
                u = chunk[1].tableCells[2].text.trim(),
                v = chunk[2].tableCells[2].text.trim()
            )
        }
    )
}