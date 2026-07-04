package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.HighScoreDao;
import at.fhooe.ald.model.HighScore;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcHighScoreDao implements HighScoreDao {
    private final Database database;

    public JdbcHighScoreDao(Database database) {
        this.database = database;
    }

    @Override
    public List<HighScore> findAll() throws SQLException {
        String sql = """
                SELECT id, player_name, floors_cleared, victory, turns_taken, created_at
                FROM high_scores
                ORDER BY victory DESC, floors_cleared DESC, turns_taken ASC, created_at DESC
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            List<HighScore> highScores = new ArrayList<>();
            while (resultSet.next()) {
                highScores.add(new HighScore(
                        resultSet.getInt("id"),
                        resultSet.getString("player_name"),
                        resultSet.getInt("floors_cleared"),
                        resultSet.getInt("victory") == 1,
                        resultSet.getInt("turns_taken"),
                        LocalDateTime.parse(resultSet.getString("created_at"))
                ));
            }
            return highScores;
        }
    }

    @Override
    public void save(HighScore highScore) throws SQLException {
        String sql = """
                INSERT INTO high_scores (player_name, floors_cleared, victory, turns_taken, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, highScore.getPlayerName());
            statement.setInt(2, highScore.getFloorsCleared());
            statement.setInt(3, highScore.isVictory() ? 1 : 0);
            statement.setInt(4, highScore.getTurnsTaken());
            statement.setString(5, highScore.getCreatedAt().toString());
            statement.executeUpdate();
        }
    }
}
