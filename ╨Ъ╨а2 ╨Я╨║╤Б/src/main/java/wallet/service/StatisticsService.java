package wallet.service;

import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;
import wallet.model.WalletStatus;
import wallet.service.filter.FilterSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Показатели считаются через Stream API по данным, загруженным из базы. */
public class StatisticsService {

    public record Turnover(BigDecimal income, BigDecimal outcome) {
    }

    public Statistics calculate(List<Client> clients, List<Wallet> wallets, List<Transaction> transactions,
                                LocalDate from, LocalDate to) {
        return calculate(clients, wallets, transactions, from, to, LocalDate.now());
    }

    Statistics calculate(List<Client> clients, List<Wallet> wallets, List<Transaction> transactions,
                         LocalDate from, LocalDate to, LocalDate today) {
        FilterSupport.checkPeriod(from, to);

        long active = countWallets(wallets, WalletStatus.ACTIVE);
        long expired = wallets.stream()
                .filter(wallet -> wallet.getStatus() != WalletStatus.CLOSED && wallet.isExpired(today))
                .count();
        BigDecimal totalBalance = sum(wallets.stream().map(Wallet::getBalance));
        Wallet richest = wallets.stream().max(Comparator.comparing(Wallet::getBalance)).orElse(null);

        List<BigDecimal> amounts = transactions.stream().map(Transaction::getAmount).toList();
        List<Transaction> inPeriod = transactions.stream()
                .filter(transaction -> FilterSupport.inPeriod(transaction.getCreatedAt().toLocalDate(), from, to))
                .toList();
        Turnover periodTurnover = turnover(inPeriod);

        return new Statistics(
                clients.size(),
                wallets.size(),
                active,
                countWallets(wallets, WalletStatus.BLOCKED),
                countWallets(wallets, WalletStatus.CLOSED),
                expired,
                wallets.isEmpty() ? null : percent(active, wallets.size()),
                totalBalance,
                average(totalBalance, wallets.size()),
                richest,
                transactions.size(),
                countTransactions(transactions, TransactionType.TOP_UP),
                countTransactions(transactions, TransactionType.TRANSFER),
                countTransactions(transactions, TransactionType.WITHDRAWAL),
                sumByType(transactions, TransactionType.TOP_UP),
                sumByType(transactions, TransactionType.TRANSFER),
                sumByType(transactions, TransactionType.WITHDRAWAL),
                average(sum(amounts.stream()), amounts.size()),
                amounts.stream().min(Comparator.naturalOrder()).orElse(null),
                amounts.stream().max(Comparator.naturalOrder()).orElse(null),
                from,
                to,
                inPeriod.size(),
                periodTurnover.income(),
                periodTurnover.outcome());
    }

    /** Поступления (пополнения) и списания (переводы и выводы) по набору операций. */
    public static Turnover turnover(Collection<Transaction> transactions) {
        BigDecimal income = sum(transactions.stream()
                .filter(transaction -> !transaction.getType().isOutgoing())
                .map(Transaction::getAmount));
        BigDecimal outcome = sum(transactions.stream()
                .filter(transaction -> transaction.getType().isOutgoing())
                .map(Transaction::getAmount));
        return new Turnover(income, outcome);
    }

    private static long countWallets(List<Wallet> wallets, WalletStatus status) {
        return wallets.stream().filter(wallet -> wallet.getStatus() == status).count();
    }

    private static long countTransactions(List<Transaction> transactions, TransactionType type) {
        return transactions.stream().filter(transaction -> transaction.getType() == type).count();
    }

    private static BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return sum(transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount));
    }

    private static BigDecimal sum(Stream<BigDecimal> values) {
        return values.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(BigDecimal total, int count) {
        return count == 0 ? null : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(long part, long total) {
        return BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
