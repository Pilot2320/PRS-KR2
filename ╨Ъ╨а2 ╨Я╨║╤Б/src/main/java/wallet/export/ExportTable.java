package wallet.export;

import java.util.List;

/** Данные для выгрузки: название листа, заголовки столбцов и строки значений. */
public record ExportTable(String title, List<String> headers, List<List<Object>> rows) {
    public ExportTable {
        headers = List.copyOf(headers);
        rows = List.copyOf(rows);
    }
}
