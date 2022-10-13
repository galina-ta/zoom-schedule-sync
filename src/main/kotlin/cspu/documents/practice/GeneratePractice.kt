package cspu.documents.practice

import cspu.documents.brs.generateBrs
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.util.CellUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat

private fun getProfile(groupName: String): String {
    return when {
        groupName.contains("099") -> {
            "«Психология образования»"
        }
        groupName.contains("227") -> {
            "«Психологическое консультирование»"
        }
        groupName.contains("224") -> {
            "«Медиация в социальной сфере»"
        }
        groupName.contains("172") -> {
            "«Психология управления образовательной средой»"
        }
        groupName.contains("270") -> {
            "«Психолого-педагогическое консультирование»"
        }
        else -> {
            throw IllegalArgumentException("Профиль не найден")
        }
    }
}

fun generatePractice(practice: Practice, brsTemplateFile: File, leoTemplateFile: File) {
    //в папке практики формируем папку, в которой будут храниться сгенерированные файлы
    val generatedDir = File(practice.dir, "Сгенерированное")
    // удаление папки перед генерацией
    generatedDir.deleteRecursively()
    //создает все элеиенты дерева папок, которые еще не созданы
    generatedDir.mkdirs()
    // для каждой группы генерируем на основе шаблона БРС
    practice.ratingByGroupName.onEach { group ->
        val groupName = group.key
        val rating = group.value
        //формируем название БРС по группе
        val brsFile = File(generatedDir, "${groupName.replace("/", "-")} БРС.xlsm")
        generateBrs(rating, brsTemplateFile, brsFile)
        //
        rating.studentNames.forEach { studentName ->
            val leoWorkbook = XSSFWorkbook(leoTemplateFile.inputStream())
            // получаем таблицу из книги
            val leoSheet = leoWorkbook.getSheetAt(0)
            practice.rpp.skills.forEachIndexed { index, skill ->
                val zRow = leoSheet.getRow(5 + index * 3)
                zRow.getCell(0).setCellValue(skill.name)
                zRow.getCell(1).setCellValue("З.${index + 1}.")
                val zTaskCell = zRow.getCell(2)
                zTaskCell.setCellValue(skill.z)
                zTaskCell.cellStyle.alignment = CellStyle.ALIGN_LEFT


                val uRow = leoSheet.getRow(5 + index * 3 + 1)
                uRow.getCell(1).setCellValue("У.${index + 1}.")
                val uTaskCell = uRow.getCell(2)
                uTaskCell.setCellValue(skill.u)
                uTaskCell.cellStyle.alignment = CellStyle.ALIGN_LEFT

                val vRow = leoSheet.getRow(5 + index * 3 + 2)
                vRow.getCell(1).setCellValue("В.${index + 1}.")
                val vTaskCell = vRow.getCell(2)
                vTaskCell.setCellValue(skill.v)
                vTaskCell.cellStyle.alignment = CellStyle.ALIGN_LEFT
            }
            val nameRow = leoSheet.getRow(0)
            val nameCell = nameRow.getCell(0)
            val format = SimpleDateFormat("dd.MM.yyyy")
            nameRow.heightInPoints = 80f
            nameCell.cellStyle.wrapText = true
            // заполняем ячейку с названием и сроками практики
            nameCell.setCellValue(
                "ЛЭО результатов обучающего ${rating.name} с ${format.format(practice.start)} по ${
                    format.format(practice.end)
                }"
            )
            // заполняем ячейку с фамилией студента
            leoSheet.getRow(1).getCell(2).setCellValue(studentName)
            val profile = getProfile(groupName)
            // заполняем ячейку с номером группы и профиля
            leoSheet.getRow(2).getCell(2).setCellValue("$groupName $profile")
            // заполняем ячейку с моей подписью
            CellUtil.getCell(leoSheet.getRow(38), 4).setCellValue("Терехова Г.В.")
            // заполняем ячейку с датой сдачи практики
            CellUtil.getCell(leoSheet.getRow(40), 1).setCellValue(format.format(practice.checkEnd))
            //формируем файлы ЛЭО для каждого студета с названием по группе и ФИО студента
            val leoFile = File(generatedDir, "${groupName.replace("/", "-")} $studentName ЛЭО.xlsx")
            leoFile.createNewFile()
            leoWorkbook.write(leoFile.outputStream())
            leoWorkbook.close()
        }
    }

}
