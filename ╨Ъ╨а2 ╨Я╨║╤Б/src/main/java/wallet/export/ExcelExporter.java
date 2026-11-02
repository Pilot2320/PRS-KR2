package wallet.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelExporter implements DataExporter {
    private static final int MAX_COLUMN_CHARS = 60;

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public String description() {
        return "Книга Excel (*.xlsx)";
    }

    @Override
    public void export(ExportTable table, Path file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(table.title()));
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            int[] widths = new int[table.headers().size()];
            Row header = sheet.createRow(0);
            for (int column = 0; column < table.headers().size(); column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(table.headers().get(column));
                cell.setCellStyle(headerStyle);
                widths[column] = table.headers().get(column).length();
            }

            int rowIndex = 1;
            for (List<Object> values : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.size(); column++) {
                    Object value = values.get(column);
                    Cell cell = row.createCell(column);
                    if (value instanceof BigDecimal number) {
                        cell.setCellValue(number.doubleValue());
                        cell.setCellStyle(moneyStyle);
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(ExportValues.text(value));
                    }
                    widths[column] = Math.max(widths[column], ExportValues.text(value).length());
                }
            }

            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, Math.min(MAX_COLUMN_CHARS, widths[column] + 3) * 256);
            }
            sheet.createFreezePane(0, 1);

            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
