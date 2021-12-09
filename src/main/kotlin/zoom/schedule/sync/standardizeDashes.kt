package zoom.schedule.sync

fun standardizeDashes(text: String): String {
    return text.replace(
        "–", // тире
        dash
    )
}

const val dash = "-" // дефис