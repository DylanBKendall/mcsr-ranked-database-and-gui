package db;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DbManager {

    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/mcsr";
    private static final String USER      = "root";
    private static final String PASSWORD  = "";

    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(JDBC_URL);
        cfg.setUsername(USER);
        cfg.setPassword(PASSWORD);

        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setIdleTimeout(60_000);
        cfg.setConnectionTimeout(30_000);
        cfg.setPoolName("McsrHikariPool");

        DATA_SOURCE = new HikariDataSource(cfg);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DATA_SOURCE.close();
            } catch (Exception ignored) {}
        }));
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void shutdown() {
        if (!DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}
