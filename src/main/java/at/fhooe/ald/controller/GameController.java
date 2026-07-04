package at.fhooe.ald.controller;

import at.fhooe.ald.model.Battle;
import at.fhooe.ald.service.GameService;
import at.fhooe.ald.service.GameStatus;
import java.sql.SQLException;
import java.util.Optional;

public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    public Battle startGame() throws SQLException {
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
}
