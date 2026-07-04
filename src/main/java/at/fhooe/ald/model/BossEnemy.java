package at.fhooe.ald.model;

import at.fhooe.ald.model.fsm.BossEnemyState;
import java.util.List;

public class BossEnemy extends Enemy {
    private BossEnemyState state;

    public BossEnemy(int id, String name, int maxHp, int currentHp, int speed,
                     String spritePath, String passiveName, String passiveDescription, List<Attack> attacks) {
        super(id, name, EnemyType.BOSS, maxHp, currentHp, speed, spritePath, passiveName, passiveDescription, attacks);
        this.state = BossEnemyState.INTRO;
    }

    public BossEnemyState getState() {
        return state;
    }

    public void setState(BossEnemyState state) {
        this.state = state;
    }
}
