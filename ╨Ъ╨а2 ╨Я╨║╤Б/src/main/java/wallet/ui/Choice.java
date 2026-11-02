package wallet.ui;

import java.util.ArrayList;
import java.util.List;

/** Пункт выпадающего списка фильтра. Значение null означает «все». */
public record Choice<T>(String title, T value) {

    public static <T> List<Choice<T>> withAll(String allTitle, List<T> values) {
        List<Choice<T>> choices = new ArrayList<>();
        choices.add(new Choice<>(allTitle, null));
        values.forEach(value -> choices.add(new Choice<>(value.toString(), value)));
        return choices;
    }

    public static <T> T valueOf(Choice<T> choice) {
        return choice == null ? null : choice.value();
    }

    @Override
    public String toString() {
        return title;
    }
}
