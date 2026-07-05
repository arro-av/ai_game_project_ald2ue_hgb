package at.fhooe.ald.dao.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;

public class DatabaseInitializer {
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final String SEED_RESOURCE = "/db/seed.sql";
    private static final String SOURCE_HASH_KEY = "source_hash";

    private final Path databasePath;

    public DatabaseInitializer() {
        this(Path.of(System.getProperty("user.home"), ".a08-dungeon-battler", "game.db"));
    }

    public DatabaseInitializer(Path databasePath) {
        this.databasePath = databasePath;
    }

    public Database initialize() throws IOException, SQLException {
        Files.createDirectories(databasePath.getParent());
        String sourceHash = databaseSourceHash();
        if (Files.notExists(databasePath)) {
            createDatabaseFromScripts(sourceHash);
        } else if (!databaseMatchesSourceHash(sourceHash)) {
            Files.delete(databasePath);
            createDatabaseFromScripts(sourceHash);
        }
        return new Database(databasePath);
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void createDatabaseFromScripts(String sourceHash) throws IOException, SQLException {
        Database database = new Database(databasePath);
        try (var connection = database.getConnection()) {
            executeScript(connection.createStatement(), readResource(SCHEMA_RESOURCE));
            executeScript(connection.createStatement(), readResource(SEED_RESOURCE));
            writeSourceHash(connection, sourceHash);
        }
    }

    private boolean databaseMatchesSourceHash(String sourceHash) throws SQLException {
        Database database = new Database(databasePath);
        try (var connection = database.getConnection();
             var statement = connection.createStatement()) {
            ensureMetadataTable(statement);
            try (var preparedStatement = connection.prepareStatement(
                    "SELECT value FROM db_metadata WHERE key = ?")) {
                preparedStatement.setString(1, SOURCE_HASH_KEY);
                var resultSet = preparedStatement.executeQuery();
                return resultSet.next() && sourceHash.equals(resultSet.getString("value"));
            }
        }
    }

    private void writeSourceHash(Connection connection, String sourceHash) throws SQLException {
        try (var statement = connection.createStatement()) {
            ensureMetadataTable(statement);
        }
        try (var preparedStatement = connection.prepareStatement(
                "INSERT OR REPLACE INTO db_metadata (key, value) VALUES (?, ?)")) {
            preparedStatement.setString(1, SOURCE_HASH_KEY);
            preparedStatement.setString(2, sourceHash);
            preparedStatement.executeUpdate();
        }
    }

    private void ensureMetadataTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS db_metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String databaseSourceHash() throws IOException {
        MessageDigest digest = sha256();
        digest.update(readResource(SCHEMA_RESOURCE).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
        digest.update(readResource(SEED_RESOURCE).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
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
