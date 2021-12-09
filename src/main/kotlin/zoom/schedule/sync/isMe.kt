package zoom.schedule.sync

fun isMe(text: String): Boolean {
    val teacher = text.filter { char -> char.isLetter() }.lowercase()
    return teacher.contains("тереховагв")
}