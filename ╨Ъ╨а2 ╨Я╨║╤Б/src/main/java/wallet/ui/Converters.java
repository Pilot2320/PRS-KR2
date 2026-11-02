package wallet.ui;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.function.Function;

public final class Converters {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private Converters() {
    }

    /** Как показывать объект в ComboBox. Обратное преобразование не нужно: списки не редактируются. */
    public static <T> StringConverter<T> of(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }

    /** Дата ДД.ММ.ГГГГ. Несуществующая дата (например, 31.02.2026) не принимается и поле очищается. */
    public static StringConverter<LocalDate> date() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : DATE.format(value);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.trim(), DATE);
                } catch (DateTimeParseException exception) {
                    return null;
                }
            }
        };
    }
}
