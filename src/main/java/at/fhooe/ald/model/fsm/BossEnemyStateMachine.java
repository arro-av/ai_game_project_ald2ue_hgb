package at.fhooe.ald.model.fsm;

import at.fhooe.ald.model.BossEnemy;
import java.util.Objects;
import java.util.Optional;

public class BossEnemyStateMachine implements StateMachine<BossEnemyState> {
    private final BossEnemy bossEnemy;

    public BossEnemyStateMachine(BossEnemy bossEnemy) {
        this.bossEnemy = Objects.requireNonNull(bossEnemy);
    }

    @Override
    public BossEnemyState getCurrentState() {
        return bossEnemy.getState();
    }

    @Override
    public Optional<String> update() {
        BossEnemyState previous = bossEnemy.getState();
        BossEnemyState next = determineState();
        if (previous == next) {
            return Optional.empty();
        }
        bossEnemy.setState(next);
        return Optional.of("BossEnemy: " + previous + " -> " + next + " because " + reason(next) + ".");
    }

    private BossEnemyState determineState() {
        if (!bossEnemy.isAlive()) {
            return BossEnemyState.DEAD;
        }
        if (bossEnemy.getState() == BossEnemyState.INTRO) {
            return BossEnemyState.PHASE_ONE;
        }
        double hpPercent = (double) bossEnemy.getCurrentHp() / bossEnemy.getMaxHp();
        if (hpPercent <= 0.30) {
            return BossEnemyState.FINAL_PHASE;
        }
        if (hpPercent <= 0.60) {
            return BossEnemyState.PHASE_TWO;
        }
        return BossEnemyState.PHASE_ONE;
    }

    private String reason(BossEnemyState state) {
        return switch (state) {
            case PHASE_ONE -> "intro finished";
            case PHASE_TWO -> "HP dropped below 60%";
            case FINAL_PHASE -> "HP dropped below 30%";
            case DEAD -> "HP dropped to 0";
            case INTRO -> "intro started";
        };
    }
}
