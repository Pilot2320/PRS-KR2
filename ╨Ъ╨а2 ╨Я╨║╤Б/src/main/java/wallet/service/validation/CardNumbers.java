package wallet.service.validation;

import java.util.random.RandomGenerator;

/** Номера карт «Мир»: префикс 2200, 16 цифр, последняя — контрольная цифра по алгоритму Луна. */
public final class CardNumbers {
    public static final String MIR_PREFIX = "2200";
    public static final int LENGTH = 16;

    private CardNumbers() {
    }

    public static String generate(RandomGenerator random) {
        StringBuilder body = new StringBuilder(MIR_PREFIX);
        while (body.length() < LENGTH - 1) {
            body.append(random.nextInt(10));
        }
        return body.toString() + checkDigit(body.toString());
    }

    public static boolean isValidLuhn(String digits) {
        if (digits == null || !digits.matches("\\d+")) {
            return false;
        }
        return luhnSum(digits, false) % 10 == 0;
    }

    /** Цифра, которую нужно дописать справа, чтобы номер прошёл проверку Луна. */
    public static int checkDigit(String body) {
        return (10 - luhnSum(body, true) % 10) % 10;
    }

    private static int luhnSum(String digits, boolean doubleRightmost) {
        int sum = 0;
        boolean doubled = doubleRightmost;
        for (int index = digits.length() - 1; index >= 0; index--) {
            int digit = digits.charAt(index) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubled = !doubled;
        }
        return sum;
    }
}
