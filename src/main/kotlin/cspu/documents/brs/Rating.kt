package cspu.documents.brs

class Rating(
    val groupName: String,
    // список студентов группы
    val studentNames: List<String>,
    // название практики или название дисциплины
    val name: String,
    val taskTypes: List<String>
)
