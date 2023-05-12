package cspu.documents

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarRequest
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import cspu.documents.lessons.Lesson
import cspu.documents.practice.Practice
import java.io.File
import java.io.InputStreamReader


// экспортировать в google-календарь
fun exportInGoogleCalendar(lessons: List<Lesson>, practices: List<Practice>) {
    // создаем объект, который позволяет общаться по сети
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    // аутентифицируемся в google-календаре и получаем доступы
    var credentials = googleAuthenticate(httpTransport)
    // если ключ доступа не получен
    if (credentials.accessToken == null) {
        //удаляем файлы аутентификации
        File(TOKENS_DIRECTORY_PATH).deleteRecursively()
        // повторно аутентифицируемся
        credentials = googleAuthenticate(httpTransport)
    }
    // конфигурируем объект, который позволяет работать с API календаря
    val calendarApi = Calendar.Builder(httpTransport, JSON_FACTORY, credentials)
        // устанавливаем название приложения, которое будет прикрепляться к каждому запросу к API
        .setApplicationName(APPLICATION_NAME)
        // создаем объект
        .build()
    // запоминаем идентификатор календаря Generated на аккаунте
    val calendarId =
        "44950e5ca587d5eefbb5721228d5d150a77d377469f558f2cde2a70afefd1af1@group.calendar.google.com"

    // события для удаления - это результат запроса всех собыий календаря
    val eventsToDelete = requestAllEvents(calendarId, calendarApi)

    //проверяем наличие событий для удаления
    if (eventsToDelete.isNotEmpty()) {
        // делаем пустую "пачку" запросов на удаление
        val deleteBatch = calendarApi.batch()
        //для каждого события на удаление
        eventsToDelete.forEach { eventToDelete ->
            // составляем запрос на удаление и добавляем его в "пачку"
            calendarApi.events().delete(calendarId, eventToDelete.id).queue(deleteBatch) {
            }
        }
        //запускаем "пачку" запросов на удаление
        deleteBatch.execute()
    }
    // создаем "пачку" для запросов на добавление собитий в календарь
    val batch = calendarApi.batch()
    // для каждого элемента расписания
    lessons.forEach { lesson ->
        // создаем событие
        val e = Event()
        // форматируем список групп, разделяя все его элементы пробелом
        val formattedGroupNames = lesson.groupNames.joinToString(separator = " ")
        // записываем в короткое описание названия дисциплины
        // и отформатированное название группы через пробел
        e.summary = "${lesson.subjectDescription} $formattedGroupNames"
        // в подробное описание добавляем из какого документа был взят этот элемент расписания
        e.description = """
            from: ${lesson.docxNames.joinToString()}
        """.trimIndent() // убирает лидирующие пробелы перед блоком текста

        // в качестве времени начала события устанавливаем время начала элемента расписания
        // и задаем часовой пояс
        e.start = EventDateTime()
            .setDateTime(DateTime(lesson.time.start))
            .setTimeZone(timeZone)
        // в качестве времени окончания события устанавливаем время конца элемента расписания
        // и задаем часовой пояс
        e.end = EventDateTime()
            .setDateTime(DateTime(lesson.time.end))
            .setTimeZone(timeZone)
        // составляем запрос на добавление события в календарь
        calendarApi.events().insert(calendarId, e).queue(batch) {
            // событие добавилось
        }
    }
    practices.forEach { practice ->
        // создаем событие
        val practiceEvent = Event()
        // форматируем список групп, разделяя все его элементы пробелом
        //val formattedGroupNames = practice.groupNames.joinToString(separator = " ")
        // записываем короткое описание названия
        practiceEvent.summary = "Практика ${practice.title}"

        val formattedGroups = practice.ratingByGroupName.entries
            .joinToString(separator = "\n\n") { entry ->
                val groupName = entry.key
                val rating = entry.value
                val formattedStudents = rating.studentNames
                    .mapIndexed { index, studentName -> "${index + 1}. $studentName" }
                    .joinToString(separator = "\n")
                "$groupName\n$formattedStudents"
            }
        // в подробное описание ...расписания
        practiceEvent.description = "$formattedGroups\n\nfrom: ${practice.docxName}"
        // в качестве времени начала события устанавливаем время начала элемента расписания
        // и задаем часовой пояс
        practiceEvent.start = EventDateTime()
            .setDateTime(DateTime(practice.start))
            .setTimeZone(timeZone)
        // в качестве времени окончания события устанавливаем время конца элемента расписания
        // и задаем часовой пояс
        practiceEvent.end = EventDateTime()
            .setDateTime(DateTime(practice.end))
            .setTimeZone(timeZone)
        // составляем запрос на добавление события в календарь
        calendarApi.events().insert(calendarId, practiceEvent).queue(batch) {
            // событие добавилось
        }
        val checkEndEvent = Event()
        checkEndEvent.summary = "срок сдачи практики ${practice.title}"
        // в подробное описание ...расписания
        checkEndEvent.description = "$formattedGroups\n\nfrom: ${practice.docxName}"
        // устанавливаем время начала срока сдачи практики
        val checkStartCalendar = java.util.Calendar.getInstance()
        checkStartCalendar.time = practice.checkEnd
        checkStartCalendar.set(java.util.Calendar.HOUR, 12)
        // в качестве времени начала события устанавливаем время начала элемента расписания
        // и задаем часовой пояс
        checkEndEvent.start = EventDateTime()
            .setDateTime(DateTime(checkStartCalendar.time))
            .setTimeZone(timeZone)
        // устанавливаем время конца срока сдачи практики
        val checkEndCalendar = java.util.Calendar.getInstance()
        checkEndCalendar.time = practice.checkEnd
        checkEndCalendar.set(java.util.Calendar.HOUR, 12)
        checkEndCalendar.set(java.util.Calendar.MINUTE, 30)
        // в качестве времени окончания события устанавливаем время конца элемента расписания
        // и задаем часовой пояс
        checkEndEvent.end = EventDateTime()
            .setDateTime(DateTime(checkEndCalendar.time))
            .setTimeZone(timeZone)
        calendarApi.events().insert(calendarId, checkEndEvent).queue(batch) {
            // событие добавилось
        }
    }
    batch.execute()
}

// запрашиваем все события, которые есть в кадендаре
private fun requestAllEvents(calendarId: String, calendarApi: Calendar): List<Event> {
    // создаём изначально пустой изменяемый список, в который потом добавим все события
    val allEvents = mutableListOf<Event>()

    // получаем первую старницу событий календаря длиной 2500 событий
    var eventsResponse = calendarApi.events()
        .list(calendarId)
        .setMaxResults(2500)
        .execute()
    // добавляем все события первой страницы в список всех событий
    allEvents.addAll(eventsResponse.items)

    // пока у последней запрошенной страницы есть ссылка на следующую:
    while (eventsResponse.nextPageToken != null) {
        // запрашиваем следующую страницу длиной 2500 событий
        eventsResponse = calendarApi.events()
            .list(calendarId)
            .setPageToken(eventsResponse.nextPageToken)
            .setMaxResults(2500)
            .execute()
        // добавляем все события этой страницы в список всех событий
        allEvents.addAll(eventsResponse.items)
    }

    // возвращаем список всех событий
    return allEvents
}

// аутентифицироваться в google-календаре
private fun googleAuthenticate(httpTransport: NetHttpTransport): Credential {
    // получаем возможность считать файл с доступами из запускаемого файла программы
    val inputStream = Lesson::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
    // считываем доступы в объект
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

    // конфигурируем объект, позволяющий аутентифицироваться
    val flow =
        GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            // указываем, что токены будут храниться в папке
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            // разрешаем аутентифицироваться через браузер только при первом запуске, а не при каждом
            .setAccessType("offline")
            // создаем объект
            .build()
    // указываем, что подтверждение аутентификации должно прийти на тот компьютер,
    // на котором исполняется программа
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    // аутентифицируемся для последующей авторизации текущего пользователя
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

// название приложения, которое прикрепляется к каждому запросу к API календаря
private const val APPLICATION_NAME = "uurggpu-schedule-sync"

// объект, который позволяет преобразовывать объект-котлин в формат, понятный серверу и обратно
private val JSON_FACTORY = JacksonFactory.getDefaultInstance()

// папка, в которую складываются токены
private const val TOKENS_DIRECTORY_PATH = "tokens"

// к какой части API календаря нужен доступ
private val SCOPES = listOf(CalendarScopes.CALENDAR)

// путь к файлу с доступами внутри исполняемого приложения
private const val CREDENTIALS_FILE_PATH = "/credentials.json"

// часовой пояс
private const val timeZone = "Asia/Yekaterinburg"

private fun <T> CalendarRequest<T>.queue(batch: BatchRequest, action: (T) -> Unit) {
    queue(batch, object : JsonBatchCallback<T>() {

        override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
            println("Error Message: " + e.message)
        }

        override fun onSuccess(t: T, responseHeaders: HttpHeaders?) {
            action(t)
        }
    })
}