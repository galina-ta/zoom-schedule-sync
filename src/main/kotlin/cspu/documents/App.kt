package cspu.documents

import cspu.documents.lessons.exportInGoogleCalendar
import cspu.documents.lessons.generateLessonsRating
import cspu.documents.lessons.loadLessons
import cspu.documents.practice.generatePractice
import cspu.documents.practice.loadPractices
import java.io.File

// определяем входную точку
fun main() {
    // указываем путь к файлам
    val asuDir = File("C:\\Users\\1255865\\Documents\\кафедра\\АСУ_Галина")
    val brsTemplateFile = File(asuDir, "БРС_шаблон.xlsm")
    val leoTemplateFile = File(asuDir, "ЛЭО_шаблон.xlsx")
    val lessonsDir = File(asuDir, "Аудиторные")
    val sessionDir = File(lessonsDir, "Сессия ОФ")
    val lessonsRatingDir = File(lessonsDir, "Рейтинг")
    val lessons = loadLessons(lessonsDir, sessionDir)
    val practicesDir = File(asuDir, "Практика")
    val practices = loadPractices(practicesDir)
    practices.forEach { practice ->
        generatePractice(practice, brsTemplateFile, leoTemplateFile)
    }
    generateLessonsRating(lessonsRatingDir, brsTemplateFile)
    // экспортируем расписание в google-календарь
    exportInGoogleCalendar(lessons, practices)
}