package wallet.service;

import wallet.model.Wallet;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Рассчитанные показатели. Пустое значение (null) означает «нет данных для расчёта». */
public record Statistics(
        long clients,
        long wallets,
        long activeWallets,
        long blockedWallets,
        long closedWallets,
        long expiredWallets,
        BigDecimal activeShare,
        BigDecimal totalBalance,
        BigDecimal averageBalance,
        Wallet richestWallet,
        long transactions,
        long topUps,
        long transfers,
        long withdrawals,
        BigDecimal topUpSum,
        BigDecimal transferSum,
        BigDecimal withdrawalSum,
        BigDecimal averageAmount,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDate periodFrom,
        LocalDate periodTo,
        long periodTransactions,
        BigDecimal periodIncome,
        BigDecimal periodOutcome) {

    public record Indicator(String title, String value, String note) {
    }

    public BigDecimal outgoingSum() {
        return transferSum.add(withdrawalSum);
    }

    public String periodTitle() {
        if (periodFrom == null && periodTo == null) {
            return "за всё время";
        }
        if (periodFrom == null) {
            return "по " + Formats.date(periodTo);
        }
        if (periodTo == null) {
            return "с " + Formats.date(periodFrom);
        }
        return Formats.date(periodFrom) + " — " + Formats.date(periodTo);
    }

    /** Показатели в том порядке, в каком они выводятся на экран и в файл экспорта. */
    public List<Indicator> indicators() {
        return List.of(
                new Indicator("Клиентов", String.valueOf(clients), "зарегистрировано в системе"),
                new Indicator("Кошельков", String.valueOf(wallets), "активных " + activeWallets
                        + " · заблокировано " + blockedWallets + " · закрыто " + closedWallets),
                new Indicator("Доля активных кошельков", Formats.percent(activeShare),
                        activeWallets + " из " + wallets),
                new Indicator("Просроченных кошельков", String.valueOf(expiredWallets),
                        "срок карты истёк, операции недоступны"),
                new Indicator("Общий баланс", Formats.money(totalBalance), "сумма остатков на всех кошельках"),
                new Indicator("Средний баланс", Formats.money(averageBalance), richestWallet == null
                        ? "кошельков нет"
                        : "максимальный: " + Formats.money(richestWallet.getBalance()) + " (" + richestWallet.getWalletNumber() + ")"),
                new Indicator("Операций всего", String.valueOf(transactions), "пополнений " + topUps
                        + " · переводов " + transfers + " · выводов " + withdrawals),
                new Indicator("Сумма пополнений", Formats.money(topUpSum),
                        Formats.plural(topUps, "операция", "операции", "операций")),
                new Indicator("Сумма списаний", Formats.money(outgoingSum()), "переводы "
                        + Formats.money(transferSum) + " + выводы " + Formats.money(withdrawalSum)),
                new Indicator("Средняя сумма операции", Formats.money(averageAmount), "мин. "
                        + Formats.money(minAmount) + " · макс. " + Formats.money(maxAmount)),
                new Indicator("Операций за период", String.valueOf(periodTransactions), periodTitle()),
                new Indicator("Поступления за период", Formats.money(periodIncome),
                        "списания: " + Formats.money(periodOutcome)));
    }
}
