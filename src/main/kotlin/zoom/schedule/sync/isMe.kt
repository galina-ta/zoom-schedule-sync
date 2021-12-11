package zoom.schedule.sync

// проверяем содержит ли текст мою фамилию и инициалы
fun isMe(text: String): Boolean {
    // выбираем из текста все буквы, входящие в текст и приводим его к нижнему регистру
    val clearText = text.filter { char -> char.isLetter() }.lowercase()
    // проверяем содержится ли в тексте тереховагв
    return clearText.contains("тереховагв")
}