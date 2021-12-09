package zoom.schedule.sync

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.text.SimpleDateFormat

fun parseFull(document: XWPFDocument, docxName: String): List<ScheduleEntry> {
    return document.tables.flatMap { table ->
        val groups = table.rows[0].tableCells.mapNotNull { cell ->
            if (isGroupName(text = cell.text)) {
                Group(name = cell.text.trim(), cellWidth = cellWidth(cell))
            } else {
                null
            }
        }
        var currentDay: String? = null
        table.rows.flatMap { row ->
            val rowDay = row.tableCells[0].text.filter { char -> char.isDigit() || char == '.' }
            if (rowDay.isNotBlank()) {
                currentDay = rowDay
            }
            val time = standardizeDashes(text = row.tableCells[1].text).replace(".", ":")
            val startTime = time.substringBefore(dash).trim()
            val endTime = time.substringAfter(dash).trim()
            val formattedStart = "$currentDay $startTime"
            val formattedEnd = "$currentDay $endTime"
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
            var currentCellIndex = 2

            var commonSubjectName: String? = null
            var commonSubjectCellWidth = 0
            for (cell in row.tableCells.drop(2)) {
                val subjectName = cell.text.trim()
                if (subjectName.isNotEmpty()) {
                    if (commonSubjectName != null) {
                        break
                    }
                    commonSubjectName = subjectName
                    val cellWidth = cellWidth(cell)
                    if (commonSubjectCellWidth < cellWidth) {
                        commonSubjectCellWidth = cellWidth
                    }
                }
            }

            if (row.tableCells.drop(2).dropLast(1).all { cell -> cell.text.isBlank() }) {
                emptyList()
            } else {
                if (groups.size == 2 &&
                    commonSubjectName != null &&
                    groups[0].cellWidth < commonSubjectCellWidth && groups[1].cellWidth < commonSubjectCellWidth
                ) {
                    if (isMe(text = commonSubjectName)) {
                        listOf(
                            ScheduleEntry(
                                start = format.parse(formattedStart),
                                end = format.parse(formattedEnd),
                                groupNames = listOf(groups[0].name, groups[1].name),
                                subjectName = "${clearSubjectName(commonSubjectName)} ${row.tableCells.last().text.trim()}",
                                docxName = docxName
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    if (row.tableCells.any { cell -> isMe(cell.text) }) {
                        groups.flatMap { group ->
                            val subjectNames = mutableListOf<String>()
                            var currentSubjectCellsWidth = 0
                            while (currentSubjectCellsWidth < group.cellWidth) {
                                val currentCell = row.tableCells[currentCellIndex]
                                currentSubjectCellsWidth += cellWidth(cell = currentCell)
                                currentCellIndex += 1
                                val subjectName = currentCell.text.trim()
                                if (isMe(subjectName)) {
                                    val roomName = row.tableCells.drop(currentCellIndex)
                                        .firstOrNull { cell -> cell.text.isNotBlank() }
                                        ?.text?.trim()
                                    if (roomName != null) {
                                        subjectNames.add("${clearSubjectName(subjectName)} $roomName")
                                    } else {
                                        subjectNames.add(clearSubjectName(subjectName))
                                    }
                                }
                            }
                            subjectNames.map { subjectName ->
                                ScheduleEntry(
                                    start = format.parse(formattedStart),
                                    end = format.parse(formattedEnd),
                                    groupNames = listOf(group.name),
                                    subjectName = subjectName,
                                    docxName = docxName
                                )
                            }
                        }
                    } else {
                        emptyList()
                    }
                }
            }
        }
    }
}

private fun clearSubjectName(subjectName: String): String {
    return subjectName.substringBefore("Терехова").trim { char ->
        char.isWhitespace() || char == ',' || char == '.'
    }
}