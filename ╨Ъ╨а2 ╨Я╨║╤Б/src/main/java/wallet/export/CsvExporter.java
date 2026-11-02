package wallet.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CSV в UTF-8 с BOM и разделителем «;» — так файл корректно открывается в Excel с русской локалью. */
public class CsvExporter implements DataExporter {
    private static final String SEPARATOR = ";";
    private static final String LINE_END = "\r\n";

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public String description() {
        return "CSV, разделитель «;» (*.csv)";
    }

    @Override
    public void export(ExportTable table, Path file) throws IOException {
        StringBuilder content = new StringBuilder("\uFEFF");
        appendLine(content, table.headers());
        for (List<Object> row : table.rows()) {
            appendLine(content, row);
        }
        Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
    }

    private void appendLine(StringBuilder content, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                content.append(SEPARATOR);
            }
            content.append(escape(ExportValues.text(values.get(index))));
        }
        content.append(LINE_END);
    }

    private String escape(String value) {
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
