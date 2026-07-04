package at.fhooe.ald.dao.jdbc;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final Path databasePath;

    public Database(Path databasePath) {
        this.databasePath = databasePath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    public Path getDatabasePath() {
        return databasePath;
    }
}
