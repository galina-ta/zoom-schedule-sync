package cspu.documents.brs

import org.apache.poi.ss.util.CellUtil
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

fun generateBrs(rating: Rating, brsTemplateFile: File, brsFile: File) {
    //  загружаем из файла книгу excel
    val brsWorkbook = XSSFWorkbook(brsTemplateFile.inputStream())
    // получаем таблицу из книги
    val brsSheet = brsWorkbook.getSheetAt(0)
    // заполняем ячейку с названием практики
    brsSheet.getRow(0).getCell(2).setCellValue(rating.name)
    // заполняем ячейку с номером группы
    brsSheet.getRow(1).getCell(2).setCellValue(rating.groupName)
    //определяем строку заданий
    val tasksRow = brsSheet.getRow(7)
    // для каждого типа заданий из РПП с его индексом
    rating.taskTypes.forEachIndexed { index, taskType ->
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
    rating.studentNames.forEachIndexed { index, student ->
        val cell = brsSheet.getRow(10 + index).getCell(1)
        cell.setCellValue(student)
        cell.cellStyle.wrapText = true
    }
    CellUtil.getCell(brsSheet.getRow(42), 12).setCellValue("Терехова Г.В.")
    if (!brsFile.exists()) {
        brsFile.createNewFile()
    }
    //
    brsWorkbook.write(brsFile.outputStream())
    brsWorkbook.close()
}