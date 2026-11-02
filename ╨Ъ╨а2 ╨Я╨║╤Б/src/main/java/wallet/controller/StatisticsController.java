package wallet.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import wallet.exception.BusinessException;
import wallet.service.ExportService;
import wallet.service.Statistics;
import wallet.service.StatisticsService;
import wallet.ui.Converters;
import wallet.ui.DataModel;
import wallet.ui.ExportDialog;
import wallet.ui.Nodes;

import java.time.LocalDate;
import java.util.List;

/** Вкладка StatisticsTab.fxml: показатели пересчитываются после каждого обновления данных и смены периода. */
public class StatisticsController {
    private static final int CARD_COLUMNS = 4;

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label periodError;
    @FXML private GridPane cards;
    @FXML private PieChart statusChart;
    @FXML private BarChart<String, Number> typeChart;

    private final DataModel dataModel;
    private final StatisticsService statisticsService;
    private final ExportService exportService;
    private final ExportDialog exportDialog;
    private Statistics statistics;

    public StatisticsController(DataModel dataModel, StatisticsService statisticsService,
                                ExportService exportService, ExportDialog exportDialog) {
        this.dataModel = dataModel;
        this.statisticsService = statisticsService;
        this.exportService = exportService;
        this.exportDialog = exportDialog;
    }

    @FXML
    private void initialize() {
        fromDate.setConverter(Converters.date());
        toDate.setConverter(Converters.date());
        fromDate.setValue(LocalDate.now().minusMonths(1));
        toDate.setValue(LocalDate.now());
        fromDate.valueProperty().addListener(observable -> recalculate());
        toDate.valueProperty().addListener(observable -> recalculate());
        dataModel.lastUpdateProperty().addListener(observable -> recalculate());
        statusChart.setAnimated(false);
        typeChart.setAnimated(false);
        typeChart.setLegendVisible(false);
        recalculate();
    }

    @FXML
    private void onLastMonth() {
        toDate.setValue(LocalDate.now());
        fromDate.setValue(LocalDate.now().minusMonths(1));
    }

    @FXML
    private void onAllTime() {
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    @FXML
    private void onExport() {
        if (statistics != null) {
            exportDialog.export(window(), exportService.statistics(statistics), "statistics");
        }
    }

    private void recalculate() {
        try {
            statistics = statisticsService.calculate(dataModel.clientEntities(), dataModel.walletEntities(),
                    dataModel.transactionEntities(), fromDate.getValue(), toDate.getValue());
            Nodes.setShown(periodError, false);
            Nodes.markInvalid(false, fromDate, toDate);
            render();
        } catch (BusinessException exception) {
            Nodes.showMessage(periodError, exception.getMessage());
            Nodes.markInvalid(true, fromDate, toDate);
        }
    }

    private void render() {
        List<Statistics.Indicator> indicators = statistics.indicators();
        cards.getChildren().clear();
        for (int index = 0; index < indicators.size(); index++) {
            cards.add(card(indicators.get(index)), index % CARD_COLUMNS, index / CARD_COLUMNS);
        }

        // Порядок секторов постоянный: цвет сектора в CSS привязан к его номеру.
        statusChart.getData().setAll(List.of(
                slice("Активные", statistics.activeWallets()),
                slice("Заблокированные", statistics.blockedWallets()),
                slice("Закрытые", statistics.closedWallets())));

        XYChart.Series<String, Number> sums = new XYChart.Series<>();
        sums.getData().add(new XYChart.Data<>("Пополнения", statistics.topUpSum()));
        sums.getData().add(new XYChart.Data<>("Переводы", statistics.transferSum()));
        sums.getData().add(new XYChart.Data<>("Выводы", statistics.withdrawalSum()));
        typeChart.getData().clear();
        typeChart.getData().add(sums);
    }

    private VBox card(Statistics.Indicator indicator) {
        Label title = new Label(indicator.title());
        title.getStyleClass().add("stat-title");
        Label value = new Label(indicator.value());
        value.getStyleClass().add("stat-value");
        Label note = new Label(indicator.note());
        note.getStyleClass().add("stat-note");
        note.setWrapText(true);
        VBox card = new VBox(4, title, value, note);
        card.getStyleClass().add("stat-card");
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private PieChart.Data slice(String title, long count) {
        return new PieChart.Data(title + " · " + count, count);
    }

    private Window window() {
        return cards.getScene().getWindow();
    }
}
