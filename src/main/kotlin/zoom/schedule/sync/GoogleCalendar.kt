package zoom.schedule.sync

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.io.File
import java.io.InputStreamReader

// экспортировать в google-календарь
fun exportInGoogleCalendar(schedule: List<ScheduleEntry>) {
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

    // запоминаем идентификатор основного календаря на аккаунте
    val calendarId = "primary"
    // конфигурируем запрос на список событий (до 99999 штук) и запускаем его
    val events = calendarApi.events().list(calendarId).setMaxResults(99999).execute()
    // для каждого полученного события
    events.items.forEach { event ->
        // составляем запрос на удаление этого события
        calendarApi.events().delete(calendarId, event.id).execute()
    }
    // для каждого элемента расписания
    schedule.forEach { scheduleEntry ->
        // создаем событие
        val e = Event()
        // форматируем список групп, разделяя все его элементы пробелом
        val formattedGroupNames = scheduleEntry.groupNames.joinToString(separator = " ")
        // записываем в короткое описание названия дисциплины и отформатированное название группы через пробел
        e.summary = "${scheduleEntry.subjectName} $formattedGroupNames"
        // в подробное описание добавляем ссылку zoom  и из какого документа был взят этот элемент расписания
        e.description = """
            https://us05web.zoom.us/j/88602486982?pwd=YnJiU21ldHl2TnRINXNpRnl3ODE5Zz09
            
            from: ${scheduleEntry.docxName}
        """.trimIndent() // убирает лидирующие пробелы перед блоком текста

        // в качестве времени начала события устанавливаем время начала элемента расписания и задаем часовой пояс
        e.start = EventDateTime()
            .setDateTime(DateTime(scheduleEntry.start))
            .setTimeZone(timeZone)
        // в качестве времени окончания события устанавливаем время конца элемента расписания и задаем часовой пояс
        e.end = EventDateTime()
            .setDateTime(DateTime(scheduleEntry.end))
            .setTimeZone(timeZone)
        // составляем запрос на добавление события в календарь
        calendarApi.events().insert(calendarId, e).execute()
    }
}

// аутентифицироваться в google-календаре
private fun googleAuthenticate(httpTransport: NetHttpTransport): Credential {
    // получаем возможность считать файл с доступами из запускаемого файла программы
    val inputStream = ScheduleEntry::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
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
    // указываем, что подтверждение аутентификации должно прийти на тот компьютер, на котором исполняется программа
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