package cspu.documents.brs

class Rating(
    val groupName: String,
    // список студентов группы
    val studentNames: List<String>,
    // название практики или название дисциплины
    val name: String,
    // список заданий из рабочей программы (для практики)
    // и последовательная нумерация для учебной дисциплины
    val taskTypes: List<String>
)