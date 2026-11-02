package wallet.service.filter;

import wallet.exception.BusinessException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FilterSupport {
    private FilterSupport() {
    }

    /**
     * Поиск без учёта регистра и разницы «е/ё». Запрос делится на слова,
     * и каждое слово должно встретиться хотя бы в одном из полей.
     */
    public static boolean containsText(String query, String... fields) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String text = normalize(Arrays.stream(fields)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));
        for (String word : normalize(query).trim().split("\\s+")) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    public static void checkPeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessException("Дата окончания периода раньше даты начала: дата «по» должна быть не раньше даты «с»");
        }
    }

    /** Границы включаются; пустая граница означает «без ограничения». */
    public static boolean inPeriod(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
