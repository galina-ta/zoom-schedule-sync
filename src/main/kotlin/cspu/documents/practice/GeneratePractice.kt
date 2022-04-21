package cspu.documents.practice

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.util.CellUtil
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat

private val practicesByNumber = mapOf(
    "20" to "Производственная практика по получению профессиональных умений и опыта профессиональной деятельности (психолого-педагогическое сопровождение профессионального образования и обучения)",
    "21" to "Производственная практика (педагогическая)",
    "25" to "Учебная практика (введение в профессию)",
    "26" to "Учебная практика (научно-исследовательская работа (получение первичных навыков научно-исследовательской работы))",
    "32" to "Учебная практика (научно-исследовательская работа (получение первичных навыков научно-исследовательской работы))",
    "29" to "Производственная практика (психолого-педагогическая)",
    "30" to "Производственная практика (технологическая (проектно-технологическая) по проектированию и организации деятельности психолого-педагогического направления)",
    "31" to "Производственная практика (технологическая (проектно-технологическая) по проектированию и организации деятельности психолого-педагогического направления)",
    "33" to "Производственная практика (научно-исследовательская работа по психолого-педагогическому сопровождению)",
    "34" to "Учебная практика (введение в профессию)",
    "36" to "Производственная практика (преддипломная)",
    "40" to "Практика по получению профессиональных умений и опыта профессиональной деятельности (социально-педагогическая деятельность)"
)

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

fun generatePractice(practice: Practice) {
    val practiceNumber = practice.dir.name.substringBefore(" ")
    val practiceName = practicesByNumber[practiceNumber]!!
    // в папке практики находим шаблон БРС, который соответствует требованиям:
    val brsTemplateFile = practice.dir.listFiles().find { file ->
        // имеет расширение xlsm
        file.extension == "xlsm"
        // если функция вернула null, то завершить программу с ошибкой
    }!!
    val leoTemplateFile = practice.dir.listFiles().find { file ->
        // имеет расширение xlsx
        file.extension == "xlsx"
        // если функция вернула null, то завершить программу с ошибкой
    }!!
    //в папке практики формируем папку, в которой будут храниться сгенерированные файлы
    val generatedDir = File(practice.dir, "Сгенерированное")
    // удаление папки перед генерацией
    generatedDir.deleteRecursively()
    //создает все элеиенты дерева папок, которые еще не созданы
    generatedDir.mkdirs()
    // для каждой группы генерируем на основе шаблона БРС
    practice.studentsByGroupName.onEach { group ->
        val groupName = group.key
        val studentNames = group.value
        //  загружаем из файла книгу excel
        val brsWorkbook = XSSFWorkbook(brsTemplateFile.inputStream())
        // получаем таблицу из книги
        val brsSheet = brsWorkbook.getSheetAt(0)
        // заполняем ячейку с названием практики
        brsSheet.getRow(0).getCell(2).setCellValue(practiceName)
        // заполняем ячейку с номером группы
        brsSheet.getRow(1).getCell(2).setCellValue(groupName)
        //определяем строку заданий
        val tasksRow = brsSheet.getRow(7)
        // для каждого типа заданий из РПП с его индексом
        practice.rpp.taskTypes.forEachIndexed { index, taskType ->
            // получаем ячейку с индексом типа задания +2 из строки заданий
            // и записываем в  нее тип задания
            val cell = tasksRow.getCell(2 + index)
            cell.setCellValue(taskType)
            cell.cellStyle.wrapText = true
            cell.cellStyle.font.boldweight = XSSFFont.BOLDWEIGHT_NORMAL
        }
        // в разделе вариативной части БРС формируем название ячейки с индивидуальными заданиями
        brsSheet.getRow(7).getCell(10).setCellValue("Индивидуальные задания")
        //заполняем список студентов в БРС
        studentNames.forEachIndexed { index, student ->
            val cell = brsSheet.getRow(10 + index).getCell(1)
            cell.setCellValue(student)
            cell.cellStyle.wrapText = true
        }
        CellUtil.getCell(brsSheet.getRow(42), 12).setCellValue("Терехова Г.В.")
        //формируем название БРС по группе
        val brsFile = File(generatedDir, "${groupName.replace("/", "-")} БРС.xlsm")
        if (!brsFile.exists()) {
            brsFile.createNewFile()
        }
        //
        brsWorkbook.write(brsFile.outputStream())
        brsWorkbook.close()
        //
        studentNames.forEach { studentName ->
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
                "ЛЭО результатов обучающего $practiceName с ${format.format(practice.start)} по ${
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
