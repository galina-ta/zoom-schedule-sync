package cspu.documents.lessons

import org.apache.poi.xwpf.usermodel.XWPFTableCell

// получаем ширину ячейки таблицы
fun cellWidth(cell: XWPFTableCell): Int {
    // получаем ширину из метаинформации ячейки
    return cell.ctTc.tcPr.tcW.w.toInt()
}