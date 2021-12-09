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
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

fun exportInGoogleCalendar(schedule: List<ScheduleEntry>) {
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val service = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, googleAuthorize(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build()

    val calendarId = "primary"
    val events = service.events().list(calendarId).setMaxResults(99999).execute()
    events.items.forEach { event ->
        service.events().delete(calendarId, event.id).execute()
    }
    schedule.forEach { scheduleEntry ->
        val e = Event()
        val formattedGroupNames = scheduleEntry.groupNames.joinToString(separator = " ")
        e.summary = "${scheduleEntry.subjectName} $formattedGroupNames"
        e.description = """
            https://us05web.zoom.us/j/88602486982?pwd=YnJiU21ldHl2TnRINXNpRnl3ODE5Zz09
            
            from: ${scheduleEntry.docxName}
        """.trimIndent()
        val startDateTime = DateTime(scheduleEntry.start)
        val start = EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone(timeZone)
        e.start = start

        val endDateTime = DateTime(scheduleEntry.end)
        val end = EventDateTime()
            .setDateTime(endDateTime)
            .setTimeZone(timeZone)
        e.end = end

        service.events().insert(calendarId, e).execute()
    }
}

private fun googleAuthorize(HTTP_TRANSPORT: NetHttpTransport): Credential {
    val `in`: InputStream =
        ScheduleEntry::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

    val flow =
        GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

private const val APPLICATION_NAME = "uurggpu-schedule-sync"
private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
private const val TOKENS_DIRECTORY_PATH = "tokens"
private val SCOPES = listOf(CalendarScopes.CALENDAR)
private const val CREDENTIALS_FILE_PATH = "/credentials.json"

private const val timeZone = "Asia/Yekaterinburg"