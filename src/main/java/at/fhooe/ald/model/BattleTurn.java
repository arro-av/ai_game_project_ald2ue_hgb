package at.fhooe.ald.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class BattleTurn {
    private final int turnNumber;
    private final String actorName;
    private final String actionName;
    private final String targetName;
    private final int amount;
    private final String message;
    private final LocalDateTime createdAt;

    public BattleTurn(int turnNumber, String actorName, String actionName, String targetName,
                      int amount, String message) {
        this.turnNumber = turnNumber;
        this.actorName = Objects.requireNonNullElse(actorName, "");
        this.actionName = Objects.requireNonNullElse(actionName, "");
        this.targetName = Objects.requireNonNullElse(targetName, "");
        this.amount = amount;
        this.message = Objects.requireNonNullElse(message, "");
        this.createdAt = LocalDateTime.now();
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getTargetName() {
        return targetName;
    }

    public int getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
