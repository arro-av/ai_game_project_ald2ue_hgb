package at.fhooe.ald.service;

import at.fhooe.ald.dao.HighScoreDao;
import at.fhooe.ald.model.HighScore;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class HighScoreService {
    private final HighScoreDao highScoreDao;

    public HighScoreService(HighScoreDao highScoreDao) {
        this.highScoreDao = highScoreDao;
    }

    public void saveRunResult(String playerName, int floorsCleared, boolean victory, int turnsTaken)
            throws SQLException {
        highScoreDao.save(new HighScore(
                0,
                playerName,
                floorsCleared,
                victory,
                turnsTaken,
                LocalDateTime.now()
        ));
    }

    public List<HighScore> findAll() throws SQLException {
        return highScoreDao.findAll();
    }
}
