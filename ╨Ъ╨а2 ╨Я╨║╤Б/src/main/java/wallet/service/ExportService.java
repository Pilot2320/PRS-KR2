package wallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.dto.ClientView;
import wallet.dto.TransactionView;
import wallet.dto.WalletView;
import wallet.exception.ExportException;
import wallet.export.DataExporter;
import wallet.export.ExportTable;
import wallet.util.Formats;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Готовит строки для выгрузки и выбирает формат файла по расширению. */
public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final List<DataExporter> exporters;

    public ExportService(List<DataExporter> exporters) {
        this.exporters = List.copyOf(exporters);
    }

    public List<DataExporter> exporters() {
        return exporters;
    }

    public ExportTable clients(List<ClientView> clients) {
        return new ExportTable("Клиенты",
                List.of("ФИО", "Email", "Телефон", "Дата регистрации", "Кошелёк", "Баланс, ₽", "Статус кошелька"),
                clients.stream().map(client -> row(
                        client.fullName(), client.email(), client.phone(), client.registeredAt(),
                        client.walletNumber(), client.balance(),
                        client.status() == null ? "" : client.status().getTitle())).toList());
    }

    public ExportTable wallets(List<WalletView> wallets) {
        return new ExportTable("Кошельки",
                List.of("Номер", "Владелец", "Держатель карты", "Карта", "Платёжная система", "Действует до",
                        "Баланс, ₽", "Остаток лимита на сегодня, ₽", "Статус"),
                wallets.stream().map(wallet -> row(
                        wallet.number(), wallet.ownerName(), wallet.holderName(), wallet.maskedCard(),
                        wallet.wallet().getPaymentSystem().getTitle(), Formats.month(wallet.expiryDate()),
                        wallet.balance(), wallet.limitLeft(), wallet.status().getTitle())).toList());
    }

    public ExportTable transactions(List<TransactionView> transactions) {
        return new ExportTable("Операции",
                List.of("Дата и время", "Кошелёк", "Владелец", "Тип", "Сумма, ₽", "Описание", "Получатель"),
                transactions.stream().map(transaction -> row(
                        transaction.createdAt(), transaction.walletNumber(), transaction.ownerName(),
                        transaction.type().getTitle(), transaction.amount(), transaction.description(),
                        transaction.counterparty())).toList());
    }

    public ExportTable statistics(Statistics statistics) {
        return new ExportTable("Статистика",
                List.of("Показатель", "Значение", "Пояснение"),
                statistics.indicators().stream()
                        .map(indicator -> row(indicator.title(), indicator.value(), indicator.note()))
                        .toList());
    }

    public void export(ExportTable table, Path file) {
        DataExporter exporter = exporterFor(file);
        try {
            exporter.export(table, file);
            log.info("Экспорт «{}»: {} строк в файл {}", table.title(), table.rows().size(), file);
        } catch (IOException exception) {
            log.error("Ошибка экспорта в файл {}", file, exception);
            throw new ExportException("Не удалось сохранить файл " + file.getFileName()
                    + ". Проверьте, что файл не открыт в другой программе и папка доступна для записи", exception);
        }
    }

    public DataExporter exporterFor(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return exporters.stream()
                .filter(exporter -> name.endsWith("." + exporter.extension()))
                .findFirst()
                .orElseThrow(() -> new ExportException("Неизвестный формат файла. Выберите расширение .csv или .xlsx"));
    }

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }
}
