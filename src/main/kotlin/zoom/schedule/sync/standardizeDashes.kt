package zoom.schedule.sync

// приводим все черточки в тексте к стандартному виду
fun standardizeDashes(text: String): String {
    // заменем все тире на дефисы
    return text.replace(
        "–", // тире
        dash
    )
}

// стандартное представление черточки
const val dash = "-" // дефис