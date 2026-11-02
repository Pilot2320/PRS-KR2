package wallet.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Параметры подключения из database.properties. Переменные окружения DB_URL, DB_USER, DB_PASSWORD важнее файла. */
public record DatabaseConfig(String url, String user, String password) {
    private static final String RESOURCE = "/database.properties";

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Не найден файл database.properties в src/main/resources");
            }
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать database.properties", exception);
        }
        return new DatabaseConfig(
                value(properties, "db.url", "DB_URL"),
                value(properties, "db.user", "DB_USER"),
                value(properties, "db.password", "DB_PASSWORD"));
    }

    private static String value(Properties properties, String key, String environmentVariable) {
        String fromEnvironment = System.getenv(environmentVariable);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("В database.properties не задан параметр " + key);
        }
        return value.trim();
    }

    /** Адрес базы без префикса JDBC и параметров, например localhost:5433/wallet_db. */
    public String displayName() {
        String address = url.replaceFirst("^jdbc:postgresql://", "");
        int query = address.indexOf('?');
        return query >= 0 ? address.substring(0, query) : address;
    }

    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=***]";
    }
}
