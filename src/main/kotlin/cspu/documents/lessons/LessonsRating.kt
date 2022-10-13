package cspu.documents.lessons

import cspu.documents.brs.Rating
import cspu.documents.brs.generateBrs
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

fun generateLessonsRating(dir: File, brsTemplateFile: File) {
    dir.listFiles()!!.forEach { classDir ->
        //в папке дисциплины формируем папку, в которой будут храниться сгенерированные файлы
        val generatedDir = File(classDir, "Сгенерированное")
        // удаление папки перед генерацией
        generatedDir.deleteRecursively()
        //создает все элеиенты дерева папок, которые еще не созданы
        generatedDir.mkdirs()
        classDir.listFiles()!!.forEach { studentsDocxFile ->
            if (studentsDocxFile.isFile) {
                // получаем документв формате библиотеки poi, передав ей возможность считать содержание файла
                val document = XWPFDocument(studentsDocxFile.inputStream())
                val studentsTable = document.tables[0]
                val studentNames = studentsTable.rows.drop(1).map { studentRow ->
                    studentRow.tableCells[1].text.trim()
                }
                val groupName = studentsDocxFile.nameWithoutExtension
                val rating = Rating(
                    groupName,
                    studentNames,
                    name = classDir.name,
                    taskTypes = listOf(
                        // надписи столбцов БРС основных заданий
                        "1", "2", "3", "4", "5", "6", "7", "",
                        // надписи столбцов БРС вариантивных заданий
                        "B1", "", ""
                    )
                )
                val brsFile = File(
                    generatedDir,
                    "${classDir.name} ${groupName}.xlsm"
                )
                generateBrs(rating, brsTemplateFile, brsFile)
            }
        }
    }
}
