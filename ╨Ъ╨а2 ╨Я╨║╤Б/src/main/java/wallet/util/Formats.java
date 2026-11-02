package wallet.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Единый формат денег и дат для таблиц, сообщений и экспорта. */
public final class Formats {
    public static final String EMPTY = "—";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Formats() {
    }

    /** 6500.5 → «6 500,50» (разделитель разрядов — неразрывный пробел). */
    public static String amount(BigDecimal value) {
        if (value == null) {
            return EMPTY;
        }
        return decimalFormat("#,##0.00").format(value);
    }

    public static String money(BigDecimal value) {
        return value == null ? EMPTY : amount(value) + " ₽";
    }

    public static String signedMoney(BigDecimal value, boolean outgoing) {
        return (outgoing ? "− " : "+ ") + money(value);
    }

    public static String percent(BigDecimal value) {
        return value == null ? EMPTY : decimalFormat("#,##0.0").format(value) + " %";
    }

    public static String date(LocalDate value) {
        return value == null ? EMPTY : DATE.format(value);
    }

    public static String dateOnly(LocalDateTime value) {
        return value == null ? EMPTY : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? EMPTY : DATE_TIME.format(value);
    }

    public static String month(LocalDate value) {
        return value == null ? EMPTY : MONTH.format(value);
    }

    public static String time(LocalDateTime value) {
        return value == null ? EMPTY : TIME.format(value);
    }

    /** Число со словом в нужной форме: 1 клиент, 3 клиента, 11 клиентов. */
    public static String plural(long count, String one, String few, String many) {
        long lastTwo = Math.abs(count) % 100;
        long last = lastTwo % 10;
        String word;
        if (lastTwo >= 11 && lastTwo <= 14) {
            word = many;
        } else if (last == 1) {
            word = one;
        } else if (last >= 2 && last <= 4) {
            word = few;
        } else {
            word = many;
        }
        return count + " " + word;
    }

    private static DecimalFormat decimalFormat(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('\u00A0');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat(pattern, symbols);
    }
}
