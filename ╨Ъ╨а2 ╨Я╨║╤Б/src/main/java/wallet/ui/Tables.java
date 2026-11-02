package wallet.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Настройка столбцов и поведения таблиц, общая для всех вкладок. */
public final class Tables {
    private Tables() {
    }

    /** Значение ячейки берётся из объекта строки. Сортировка идёт по самому значению. */
    public static <S, T> void bind(TableColumn<S, T> column, Function<S, T> value) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    }

    /** Сортировка по значению, а показ — в отформатированном виде (деньги, даты). */
    public static <S, T> void bind(TableColumn<S, T> column, Function<S, T> value, Function<T, String> format) {
        bind(column, value);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.apply(item));
            }
        });
    }

    /** Значение показывается цветной «плашкой» (статус кошелька, тип операции). */
    public static <S, T> void badge(TableColumn<S, T> column, Function<S, T> value,
                                    Function<T, String> text, Function<T, String> styleClass) {
        bind(column, value);
        column.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("badge");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(text.apply(item));
                Styles.setBadge(badge, styleClass.apply(item));
                setGraphic(badge);
            }
        });
    }

    /** Ячейка, которой для показа нужна вся строка (например, сумма со знаком зависит от типа операции). */
    public static <S> void row(TableColumn<S, S> column, Function<S, String> text, Function<S, String> styleClass,
                               Comparator<S> comparator) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setComparator(comparator);
        column.setCellFactory(col -> new TableCell<>() {
            private String appliedClass;

            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                if (appliedClass != null) {
                    getStyleClass().remove(appliedClass);
                    appliedClass = null;
                }
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(text.apply(item));
                appliedClass = styleClass.apply(item);
                if (appliedClass != null) {
                    getStyleClass().add(appliedClass);
                }
            }
        });
    }

    public static <S> void onDoubleClick(TableView<S> table, Consumer<S> action) {
        table.setRowFactory(view -> {
            TableRow<S> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /**
     * После reload() список заменяется целиком и выделение пропадает.
     * Запоминаем ID выбранной строки и выделяем её снова, если она осталась в таблице.
     */
    public static <S> void keepSelection(TableView<S> table, ToLongFunction<S> id) {
        long[] selectedId = {-1};
        table.getSelectionModel().selectedItemProperty().addListener((observable, old, item) -> {
            if (item != null) {
                selectedId[0] = id.applyAsLong(item);
            }
        });
        table.getItems().addListener((ListChangeListener<S>) change -> {
            if (table.getSelectionModel().getSelectedItem() == null && selectedId[0] >= 0) {
                select(table, item -> id.applyAsLong(item) == selectedId[0]);
            }
        });
    }

    /** Выделяет первую подходящую строку и прокручивает к ней. */
    public static <S> boolean select(TableView<S> table, Predicate<S> condition) {
        for (S item : table.getItems()) {
            if (condition.test(item)) {
                table.getSelectionModel().select(item);
                table.scrollTo(item);
                return true;
            }
        }
        return false;
    }

    public static boolean isDeleteKey(KeyEvent event) {
        return event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE;
    }
}
