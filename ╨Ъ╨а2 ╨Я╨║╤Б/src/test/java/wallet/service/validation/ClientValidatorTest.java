package wallet.service.validation;

import org.junit.jupiter.api.Test;
import wallet.dto.ClientForm;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientValidatorTest {

    @Test
    void acceptsCorrectClient() {
        ClientForm form = ClientValidator.normalize(
                new ClientForm("  Римский-Корсаков Николай Андреевич ", " Nikolay@Mail.RU ", "8 (900) 111-00-01"));

        assertThat(ClientValidator.validate(form)).isEmpty();
        assertThat(form.fullName()).isEqualTo("Римский-Корсаков Николай Андреевич");
        assertThat(form.email()).isEqualTo("nikolay@mail.ru");
        assertThat(form.phone()).isEqualTo("+79001110001");
    }

    @Test
    void reportsEveryInvalidFieldSeparately() {
        Map<String, String> errors = ClientValidator.validate(
                ClientValidator.normalize(new ClientForm("", "not-an-email", "12345")));

        assertThat(errors).containsOnlyKeys(ClientForm.FULL_NAME, ClientForm.EMAIL, ClientForm.PHONE);
        assertThat(errors.get(ClientForm.FULL_NAME)).isEqualTo("Введите ФИО клиента");
    }

    @Test
    void rejectsBadFullNames() {
        assertThat(ClientValidator.nameError("Иван")).hasValueSatisfying(message -> assertThat(message).contains("фамилию и имя"));
        assertThat(ClientValidator.nameError("иванов иван")).isPresent();
        assertThat(ClientValidator.nameError("Иванов  Иван")).isPresent();
        assertThat(ClientValidator.nameError("Иванов Иван2")).isPresent();
        assertThat(ClientValidator.nameError("Иванов Иван Иванович Младший")).isPresent();
        assertThat(ClientValidator.nameError("Иванов " + "И".repeat(150))).hasValueSatisfying(
                message -> assertThat(message).contains("150"));
    }

    @Test
    void rejectsBadPhones() {
        assertThat(ClientValidator.phoneError(ClientValidator.normalizePhone("+7900111000"))).isPresent();
        assertThat(ClientValidator.phoneError(ClientValidator.normalizePhone("+1 900 111 00 01"))).isPresent();
        assertThat(ClientValidator.phoneError(ClientValidator.normalizePhone("79001110001"))).isEmpty();
    }
}
