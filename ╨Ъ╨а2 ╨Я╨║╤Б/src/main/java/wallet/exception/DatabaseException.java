package wallet.exception;

import java.sql.SQLException;

public class DatabaseException extends AppException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Переводит код ошибки PostgreSQL (SQLState) в понятное пользователю объяснение. */
    public static DatabaseException of(String action, SQLException cause) {
        String state = cause.getSQLState() == null ? "" : cause.getSQLState();
        String reason;
        if (state.startsWith("08")) {
            reason = "нет подключения к PostgreSQL. Запустите Docker и выполните в папке проекта "
                    + "команду docker compose up -d, затем нажмите «Обновить данные»";
        } else if (state.equals("28P01") || state.equals("28000")) {
            reason = "неверный пользователь или пароль базы данных (файл database.properties)";
        } else if (state.equals("3D000")) {
            reason = "база данных не найдена, проверьте db.url в database.properties";
        } else if (state.equals("42P01")) {
            reason = "в базе нет нужных таблиц, выполните скрипт sql/schema.sql";
        } else if (state.equals("23505")) {
            reason = "такая запись уже существует";
        } else if (state.equals("23503")) {
            reason = "запись связана с другими данными";
        } else if (state.startsWith("23")) {
            reason = "данные не прошли проверку ограничений базы данных";
        } else {
            reason = "ошибка базы данных, подробности записаны в журнал";
        }
        return new DatabaseException(action + ": " + reason + ".", cause);
    }
}
