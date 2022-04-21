package cspu.documents.practice

import java.io.File
import java.util.*

class Practice(
    // начало практики
    val start: Date,
    // конец практики
    val end: Date,
    //  срок проверки документации
    val checkEnd: Date,
    //список студентов группы
    val studentsByGroupName: Map<String, List<String>>,
    // название группы
    val docxName: String,
    // название документа, из которго берем данные
    val name: String,
    // название РПП
    val rpp: Rpp,
    // директория практики, где генерируются на основе шаблонов документы
    val dir: File
)
