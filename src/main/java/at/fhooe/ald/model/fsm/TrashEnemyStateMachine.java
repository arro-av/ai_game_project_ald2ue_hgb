package at.fhooe.ald.model.fsm;

import at.fhooe.ald.model.TrashEnemy;
import java.util.Objects;
import java.util.Optional;

public class TrashEnemyStateMachine implements StateMachine<TrashEnemyState> {
    private final TrashEnemy trashEnemy;

    public TrashEnemyStateMachine(TrashEnemy trashEnemy) {
        this.trashEnemy = Objects.requireNonNull(trashEnemy);
    }

    @Override
    public TrashEnemyState getCurrentState() {
        return trashEnemy.getState();
    }

    @Override
    public Optional<String> update() {
        TrashEnemyState previous = trashEnemy.getState();
        TrashEnemyState next = determineState();
        if (previous == next) {
            return Optional.empty();
        }
        trashEnemy.setState(next);
        return Optional.of("TrashEnemy: " + previous + " -> " + next + " because " + reason(next) + ".");
    }

    private TrashEnemyState determineState() {
        if (!trashEnemy.isAlive()) {
            return TrashEnemyState.DEAD;
        }
        double hpPercent = (double) trashEnemy.getCurrentHp() / trashEnemy.getMaxHp();
        if (hpPercent <= 0.25) {
            return TrashEnemyState.DESPERATE;
        }
        if (hpPercent <= 0.60) {
            return TrashEnemyState.AGGRESSIVE;
        }
        return TrashEnemyState.NORMAL;
    }

    private String reason(TrashEnemyState state) {
        return switch (state) {
            case AGGRESSIVE -> "HP dropped below 60%";
            case DESPERATE -> "HP dropped below 25%";
            case DEAD -> "HP dropped to 0";
            case NORMAL -> "HP is above 60%";
        };
    }
}
