package wallet.ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import wallet.export.DataExporter;
import wallet.export.ExportTable;
import wallet.service.ExportService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

/** Выбор файла через FileChooser и запуск экспорта. Папка по умолчанию — «Документы» текущего пользователя. */
public class ExportDialog {
    private final ExportService exportService;
    private File lastDirectory;

    public ExportDialog(ExportService exportService) {
        this.exportService = exportService;
    }

    public void export(Window owner, ExportTable table, String fileName) {
        if (table.rows().isEmpty()) {
            Dialogs.info(owner, "Нечего экспортировать",
                    "В таблице нет строк. Измените условия поиска или сбросьте фильтры.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт: " + table.title());
        for (DataExporter exporter : exportService.exporters()) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(exporter.description(), "*." + exporter.extension()));
        }
        chooser.setInitialDirectory(initialDirectory());
        chooser.setInitialFileName(fileName + "-" + LocalDate.now() + "." + exportService.exporters().get(0).extension());

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        Path path = withExtension(file.toPath(), chooser.getSelectedExtensionFilter());
        if (Dialogs.attempt(owner, "Экспорт не выполнен", () -> exportService.export(table, path))) {
            Dialogs.info(owner, "Экспорт завершён",
                    "Сохранено строк: " + table.rows().size() + "\nФайл: " + path.toAbsolutePath());
        }
    }

    private File initialDirectory() {
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            return lastDirectory;
        }
        File home = new File(System.getProperty("user.home"));
        File documents = new File(home, "Documents");
        return documents.isDirectory() ? documents : home;
    }

    /** Если пользователь не указал расширение, берём его из выбранного типа файла. */
    private Path withExtension(Path path, FileChooser.ExtensionFilter filter) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean known = exportService.exporters().stream().anyMatch(exporter -> name.endsWith("." + exporter.extension()));
        if (known || filter == null || filter.getExtensions().isEmpty()) {
            return path;
        }
        String extension = filter.getExtensions().get(0).replace("*", "");
        return path.resolveSibling(path.getFileName() + extension);
    }
}
