package wallet.export;

import java.io.IOException;
import java.nio.file.Path;

public interface DataExporter {
    /** Расширение файла без точки, например csv. */
    String extension();

    /** Подпись формата в окне выбора файла. */
    String description();

    void export(ExportTable table, Path file) throws IOException;
}
