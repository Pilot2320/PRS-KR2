package wallet.export;

import wallet.util.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class ExportValues {
    private ExportValues() {
    }

    /** Текст ячейки: суммы с запятой без разделителя разрядов (так их понимает русский Excel), даты — ДД.ММ.ГГГГ. */
    static String text(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            return number.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        }
        if (value instanceof LocalDateTime dateTime) {
            return Formats.dateTime(dateTime);
        }
        if (value instanceof LocalDate date) {
            return Formats.date(date);
        }
        return value.toString();
    }
}
