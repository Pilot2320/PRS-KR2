package wallet.ui;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Вариант сортировки из выпадающего списка. Он делает то же, что щелчок по заголовку столбца:
 * задаёт столбец и направление в sortOrder таблицы, а TableView строит из них компаратор.
 */
public record SortOption<S>(String title, TableColumn<S, ?> column, TableColumn.SortType type) {

    public void applyTo(TableView<S> table) {
        column.setSortType(type);
        table.getSortOrder().setAll(List.of(column));
    }

    @Override
    public String toString() {
        return title;
    }
}
