package cspu.documents.lessons

// проверяем содержит ли текст мою фамилию и инициалы
fun containsMyNameShort(text: String): Boolean {
    // выбираем из текста все буквы, входящие в текст и приводим его к нижнему регистру
    val clearText = text.filter { char -> char.isLetter() }.lowercase()
    // проверяем содержится ли в тексте тереховагв
    return clearText.contains("тереховагв")
}

// проверяем содержит ли текст мою фамилию и инициалы или фамилию и имя-отчество
fun containsMyName(text: String): Boolean {
    // выбираем из текста все буквы, входящие в текст и приводим его к нижнему регистру
    val clearText = text.filter { char -> char.isLetter() }.lowercase()
    // проверяем содержится ли в тексте тереховагв или тереховагалинавладимировна
    return clearText.contains("тереховагалинавладимировна")
            || clearText.contains("тереховагв")
}