package at.fhooe.ald.controller;

import at.fhooe.ald.model.Battle;
import at.fhooe.ald.service.GameService;
import at.fhooe.ald.service.GameStatus;
import at.fhooe.ald.service.HighScoreService;
import java.sql.SQLException;
import java.util.Optional;

public class GameController {
    private final GameService gameService;
    private final HighScoreService highScoreService;
    private boolean runResultSaved;

    public GameController(GameService gameService, HighScoreService highScoreService) {
        this.gameService = gameService;
        this.highScoreService = highScoreService;
    }

    public Battle startGame() throws SQLException {
        runResultSaved = false;
        return gameService.startNewGame();
    }

    public Battle advanceAfterVictory() throws SQLException {
        return gameService.clearCurrentFloor();
    }

    public void gameOver() {
        gameService.markPartyDefeated();
    }

    public GameStatus refreshStatus() throws SQLException {
        return gameService.updateStatusFromCurrentBattle();
    }

    public GameStatus getStatus() {
        return gameService.getStatus();
    }

    public Optional<Battle> getCurrentBattle() {
        return gameService.getCurrentBattle();
    }

    public int getFloorsCleared() {
        return gameService.getFloorsCleared();
    }

    public int getTurnsTaken() {
        return gameService.getTurnsTaken();
    }

    public void saveRunResultIfNeeded() throws SQLException {
        if (runResultSaved || (getStatus() != GameStatus.VICTORY && getStatus() != GameStatus.GAME_OVER)) {
            return;
        }
        highScoreService.saveRunResult(
                "Player",
                gameService.getFloorsCleared(),
                getStatus() == GameStatus.VICTORY,
                gameService.getTurnsTaken()
        );
        runResultSaved = true;
    }
}
