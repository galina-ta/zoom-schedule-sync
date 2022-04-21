package cspu.documents

import cspu.documents.lessons.exportInGoogleCalendar
import cspu.documents.lessons.loadLessons
import cspu.documents.practice.generatePractice
import cspu.documents.practice.loadPractices
import java.io.File

// определяем входную точку
fun main() {
    // указываем путь к файлам
    val asuDir = File("C:\\Users\\1255865\\Documents\\кафедра\\АСУ_Галина")
    val lessonsDir = File(asuDir, "Аудиторные")
    val sessionDir = File(lessonsDir, "Сессия ОФ")
    val lessons = loadLessons(lessonsDir, sessionDir)
    val practicesDir = File(asuDir, "Практика")
    val practices = loadPractices(practicesDir)
    practices.forEach { practice ->
        generatePractice(practice)
    }
    // экспортируем расписание в google-календарь
    exportInGoogleCalendar(lessons, practices)
}