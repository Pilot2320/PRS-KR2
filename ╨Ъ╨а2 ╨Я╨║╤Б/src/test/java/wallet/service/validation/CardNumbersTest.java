package wallet.service.validation;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumbersTest {

    @Test
    void cardsFromSeedDataPassLuhnCheck() {
        assertThat(CardNumbers.isValidLuhn("2200000000000012")).isTrue();
        assertThat(CardNumbers.isValidLuhn("2200111122223331")).isTrue();
        assertThat(CardNumbers.isValidLuhn("2200000000000013")).isFalse();
    }

    @RepeatedTest(20)
    void generatedNumberIsMirCardWithValidCheckDigit() {
        String card = CardNumbers.generate(new Random());

        assertThat(card).hasSize(CardNumbers.LENGTH).startsWith(CardNumbers.MIR_PREFIX).containsOnlyDigits();
        assertThat(CardNumbers.isValidLuhn(card)).isTrue();
    }
}
