package wallet.dto;

/** Значения полей формы клиента в том виде, в каком их ввёл пользователь. */
public record ClientForm(String fullName, String email, String phone) {
    public static final String FULL_NAME = "fullName";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
}
