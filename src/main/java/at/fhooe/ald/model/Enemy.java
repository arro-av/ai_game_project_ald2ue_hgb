package at.fhooe.ald.model;

import java.util.List;

public abstract class Enemy extends Battler {
    private final EnemyType enemyType;
    private final String passiveName;
    private final String passiveDescription;

    protected Enemy(int id, String name, EnemyType enemyType, int maxHp, int currentHp, int speed,
                    String spritePath, String passiveName, String passiveDescription, List<Attack> attacks) {
        super(id, name, maxHp, currentHp, speed, spritePath, attacks);
        this.enemyType = enemyType;
        this.passiveName = passiveName == null ? "" : passiveName;
        this.passiveDescription = passiveDescription == null ? "" : passiveDescription;
    }

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public String getPassiveName() {
        return passiveName;
    }

    public String getPassiveDescription() {
        return passiveDescription;
    }
}
