package wallet.dto;

import wallet.model.TransactionType;

/** Значения полей формы операции. Сумма — строка, её разбирает и проверяет сервис. */
public record TransactionForm(Long walletId, TransactionType type, String amount, String description,
                              Long targetWalletId) {
    public static final String WALLET = "wallet";
    public static final String TYPE = "type";
    public static final String AMOUNT = "amount";
    public static final String DESCRIPTION = "description";
    public static final String TARGET = "target";
}
