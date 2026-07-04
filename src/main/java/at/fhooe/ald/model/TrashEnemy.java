package at.fhooe.ald.model;

import at.fhooe.ald.model.fsm.TrashEnemyState;
import java.util.List;

public class TrashEnemy extends Enemy {
    private TrashEnemyState state;

    public TrashEnemy(int id, String name, int maxHp, int currentHp, int speed,
                      String spritePath, String passiveName, String passiveDescription, List<Attack> attacks) {
        super(id, name, EnemyType.TRASH, maxHp, currentHp, speed, spritePath, passiveName, passiveDescription, attacks);
        this.state = TrashEnemyState.NORMAL;
    }

    public TrashEnemyState getState() {
        return state;
    }

    public void setState(TrashEnemyState state) {
        this.state = state;
    }
}
