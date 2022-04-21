package cspu.documents.lessons

// описание группы
class Group(
   // название
    val name: String,
    // ширина ячейки (для опреления подгрупп по сумме ширин их ячеек)
    val cellWidth: Int
)

// проверяем является ли текст названием группы
fun isGroupName(text: String): Boolean {
    // проверям является ли текст названием очной группы или заочной
    return isShortGroupName(text) || isFullGroupName(text)
}

// проверяем является ли текст названием заочной группы
fun isShortGroupName(text: String): Boolean {
    // очищаем текст
    val cleared = clearGroupName(text)
    // проверяем начинается ли очищенный текст с ЗФ-
    return cleared.startsWith("ЗФ$dash")
}

// проверяем является ли текст названием очной группы
fun isFullGroupName(text: String): Boolean {
    // очищаем текст
    val cleared = clearGroupName(text)
    // проверяем начинается ли очищенный текст с ОФ-
    return cleared.startsWith("ОФ$dash")
}

// очищаем текст названия группы
private fun clearGroupName(text: String): String {
    // стандартизуем черточки, очищаем от пробельных символов и приводимк верхнему регистру
    return standardizeDashes(text).filter { char -> !char.isWhitespace() }.uppercase()
}