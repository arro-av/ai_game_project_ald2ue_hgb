package at.fhooe.ald.dao;

import at.fhooe.ald.model.HighScore;
import java.sql.SQLException;
import java.util.List;

public interface HighScoreDao {
    List<HighScore> findAll() throws SQLException;

    void save(HighScore highScore) throws SQLException;
}
