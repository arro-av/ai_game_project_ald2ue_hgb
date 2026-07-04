package at.fhooe.ald.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class HighScore {
    private final int id;
    private final String playerName;
    private final int floorsCleared;
    private final boolean victory;
    private final int turnsTaken;
    private final LocalDateTime createdAt;

    public HighScore(int id, String playerName, int floorsCleared, boolean victory, int turnsTaken,
                     LocalDateTime createdAt) {
        this.id = id;
        this.playerName = Objects.requireNonNullElse(playerName, "Player");
        this.floorsCleared = floorsCleared;
        this.victory = victory;
        this.turnsTaken = turnsTaken;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getFloorsCleared() {
        return floorsCleared;
    }

    public boolean isVictory() {
        return victory;
    }

    public int getTurnsTaken() {
        return turnsTaken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
