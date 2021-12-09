package zoom.schedule.sync

class Group(
    val name: String,
    val cellWidth: Int
)

fun isGroupName(text: String): Boolean {
    return isShortGroupName(text) || isFullGroupName(text)
}

fun isShortGroupName(text: String): Boolean {
    val cleared = clearGroupName(text)
    return cleared.startsWith("ЗФ$dash")
}

fun isFullGroupName(text: String): Boolean {
    val cleared = clearGroupName(text)
    return cleared.startsWith("ОФ$dash")
}

private fun clearGroupName(text: String): String {
    return standardizeDashes(text).filter { char -> !char.isWhitespace() }.uppercase()
}