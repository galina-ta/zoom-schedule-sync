package cspu.documents.practice

import cspu.documents.brs.Rating
import java.io.File
import java.util.*

class Practice(
    // начало практики
    val start: Date,
    // конец практики
    val end: Date,
    // срок проверки документации
    val checkEnd: Date,
    // название папки с практикой
    val title: String,
    // название документа, из которго берем данные
    val docxName: String,
    // РПП
    val rpp: Rpp,
    // данные для БРС, Map - словарь, можно представить в виде списка пар ключ-значение.
    // можно представить что это шкаф с выдвижными ящиками подпись на ящике - это ключ,
    // внутри лежит значение - рейтинг со списком студентов и др.
    // Зная ключ мы можем получить значение из Map
    val ratingByGroupName: Map<String, Rating>,
    // директория практики, где генерируются на основе шаблонов документы
    val dir: File
)
