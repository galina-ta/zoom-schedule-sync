package cspu.documents.practice

import cspu.documents.brs.Rating
import cspu.documents.lessons.containsMyName
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.text.SimpleDateFormat

private val practicesByNumber = mapOf(
    "28" to "Производственная практика (технологическая (проектно-технологическая))",
    "21" to "Производственная практика (педагогическая)",
    "25" to "Учебная практика (введение в профессию)",
    "26" to "Учебная практика (научно-исследовательская работа (получение первичных навыков научно-исследовательской работы))",
    "32" to "Производственная практика (научно-исследовательская работа)",
    "29" to "Производственная практика (психолого-педагогическая)",
    "30" to "Производственная практика (психолого-педагогическая)",
    "31" to "Учебная практика (психолого-педагогическое сопровождение общего образования)",
    "33" to "Производственная практика (научно-исследовательская работа по психолого-педагогическому сопровождению)",
    "34" to "Учебная практика (введение в профессию)",
    "36" to "Учебная практика (научно-исследовательская работа (получение первичных навыков научно-исследовательской работы))",
    "39" to "Производственная практика (научно-исследовательская работа)",
    "40" to "Производственная практика (преддипломная)",
    "42" to "Производственная практика (научно-исследовательская работа)",
    "2" to "Производственная практика (педагогическая)",
    "5" to "Производственная практика (научно-исследовательская работа по профильной подготовке)",
    "6" to "Учебная практика (технологическая (проектно-технологическая) по профильной подготовке)",
    "8" to "Учебная практика (психолого-педагогическое сопровождение профессионального образования и обучения)",
    "9" to "Учебная практика (психолого-педагогическое сопровождение общего образования)",
    "14" to "Производственная практика (социально-психологическая)",
    "15" to "Производственная практика (по психолого-педагогическому сопровождению)",
    "18" to "Производственна практика (педагогическая в образовательных организациях)",
    "16" to "Учебная практика (технологическая (проектно-технологическая) по профильной подготовке)",
    "17" to "Производственная практика (педагогическая)",
    "19" to "Производственная практика (педагогическая)",
    "24" to "Производственная практика (педагогическая)",
    "35" to "Учебная практика (введение в профессию)",
    "20" to "Производственная практика (социально-психологическая)",
    "37" to "Производственная практика (преддипломная)",
    "44" to "Производственная практика (технологическая (проектно-технологическая))"
)

//
fun loadPractices(dir: File): List<Practice> {
    return dir.listFiles()!!
        .filter { dir -> dir.name != "Заполненные" && dir.isDirectory }
        .map { practiceDir ->
            // в папке практики находим документ приказа, который соответствует требованиям:
            val orderDocxFile = practiceDir.listFiles().find { file ->
                // имеет расширение docx и не содержит в названии документа РПП
                file.extension == "docx" && !file.name.contains("РПП")
                // если функция вернула null, то завершить программу с ошибкой
            }!!
            // в папке практики находим документ РПП, который соответствует требованиям:
            val rppDocxFile = practiceDir.listFiles().find { file ->
                // имеет расширение docx и не содержит в названии документа РПП
                file.extension == "docx" && file.name.contains("РПП")
            }!!
            val rpp = loadRpp(docxFile = rppDocxFile)
            // получаем документ в формате библиотеки poi, передав ей возможность считать содержание файла
            val document = XWPFDocument(orderDocxFile.inputStream())
            //среди параграфов документов выбираем
            val description = document.paragraphs
                // среди них ищем тот, который содержит слово "организовать" без учета регистра
                .first { p -> p.text.contains("организовать", ignoreCase = true) }
                .text
            //задаем описание начала сроков практики, очищаем
            val formattedStart = description
                .substringAfterLast("с ")
                .substringBefore("г")
                .trim()
            // задаем описание конца сроков практики, очищаем
            val formattedEnd = description
                .substringAfterLast("по ")
                .substringBefore("г")
                .trim()
            // задаем формат представления даты
            val format = SimpleDateFormat("dd.MM.yyyy")
            // находим последний параграф
            val checkEndParagraph =
                document.paragraphs.filter { p -> p.text.isNotBlank() }.last().text
            //выбираем информацию о сроке сдачи из последнего параграфа
            val formattedCheckEnd = checkEndParagraph
                .substringAfterLast("до")
                .trim { c -> c.isWhitespace() || c == '.' }
            val practiceNumber = practiceDir.name.substringBefore(" ")
            val practiceName = practicesByNumber[practiceNumber]!!
            // формируем рейтинги студентов
            val ratingByGroupName = document.bodyElements.mapIndexedNotNull { index, element ->
                // если элемент документа
                if (element is XWPFParagraph) {
                    // определяем название группы
                    val groupName = getGroupName(element.text)
                    //если список студентов группы не нулевой...
                    if (groupName != null) {
                        val studentsTable = document.bodyElements.drop(index + 1)
                            .filterNot { e -> e is XWPFParagraph && e.text.isBlank() }
                            .first() as XWPFTable
                        //
                        val studentsNames = studentsTable.rows.mapNotNull { row ->
                            if (row.tableCells.any { cell -> containsMyName(text = cell.text) }) {
                                row.tableCells[1].text
                                // иначе ничего не добавлять
                            } else {
                                null
                            }
                        } //если список не пустой...
                        if (studentsNames.isNotEmpty()) {
                            groupName to Rating(
                                groupName,
                                studentsNames,
                                practiceName,
                                rpp.taskTypes,
                                variationTaskType = "Индивидуальные задания",
                                customFinalTaskName = null
                            )
                            // иначе ничего не добавлять
                        } else {
                            null
                        }
                        // иначе ничего не добавлять
                    } else {
                        null
                    }
                    // иначе ничего не добавлять
                } else {
                    null
                }
            }.toMap()
            // описание записи практики
            Practice(
                // время начала практики разбираем по формату
                start = format.parse(formattedStart),
                // время конца практики разбираем по формату
                end = format.parse(formattedEnd),
                // срок сдачи практики
                checkEnd = format.parse(formattedCheckEnd),
                title = orderDocxFile.nameWithoutExtension
                    .substringBefore("проект").trim(),
                // название файла, из которого берем информацию
                docxName = orderDocxFile.name,
                // формируем название из названия документа, очищенного
                rpp = rpp,
                ratingByGroupName = ratingByGroupName,
                dir = practiceDir
            )
        }
}

//ищем название группы  в тексте по условиям:
private fun getGroupName(text: String): String? {
    // если текст содержит ЗФ-
    if (text.contains("ЗФ-")) {
        // то добавляем к ЗФ- текст после этих символов до пробела
        return "ЗФ-" + text.substringAfter("ЗФ-").substringBefore(" ")
            // очищаем от точек и запятых вначале и в конце
            .trim { c -> c == '.' || c == ',' }
        // иначе если содержит ОФ-
    } else if (text.contains("ОФ-")) {
        // то добавляем к ОФ- текст после этих символов до пробела
        return "ОФ-" + text.substringAfter("ОФ-").substringBefore(" ")
            // очищаем от точек и запятых вначале и в конце
            .trim { c -> c == '.' || c == ',' }
        // иначе
    } else {
        // возвращаем null, значит в тексте нет названия группы
        return null
    }
}