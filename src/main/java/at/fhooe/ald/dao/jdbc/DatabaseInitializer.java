package at.fhooe.ald.dao.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final String DB_RESOURCE = "/db/game.db";
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final String SEED_RESOURCE = "/db/seed.sql";

    private final Path databasePath;

    public DatabaseInitializer() {
        this(Path.of(System.getProperty("user.home"), ".a08-dungeon-battler", "game.db"));
    }

    public DatabaseInitializer(Path databasePath) {
        this.databasePath = databasePath;
    }

    public Database initialize() throws IOException, SQLException {
        Files.createDirectories(databasePath.getParent());
        if (Files.notExists(databasePath)) {
            copyBundledDatabaseOrCreateFromScripts();
        }
        return new Database(databasePath);
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void copyBundledDatabaseOrCreateFromScripts() throws IOException, SQLException {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(DB_RESOURCE)) {
            if (inputStream != null) {
                Files.copy(inputStream, databasePath);
                return;
            }
        }

        Database database = new Database(databasePath);
        try (var connection = database.getConnection()) {
            executeScript(connection.createStatement(), readResource(SCHEMA_RESOURCE));
            executeScript(connection.createStatement(), readResource(SEED_RESOURCE));
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void executeScript(Statement statement, String script) throws SQLException {
        try (statement) {
            for (String sql : script.split(";")) {
                String cleaned = removeLineComments(sql).trim();
                if (!cleaned.isBlank()) {
                    statement.execute(cleaned);
                }
            }
        }
    }

    private String removeLineComments(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                result.append(line).append(System.lineSeparator());
            }
        }
        return result.toString();
    }
}
