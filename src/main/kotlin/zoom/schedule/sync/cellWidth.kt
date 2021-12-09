package zoom.schedule.sync

import org.apache.poi.xwpf.usermodel.XWPFTableCell

fun cellWidth(cell: XWPFTableCell): Int {
    return cell.ctTc.tcPr.tcW.w.toInt()
}