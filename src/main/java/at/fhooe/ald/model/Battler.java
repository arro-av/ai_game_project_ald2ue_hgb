package at.fhooe.ald.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Battler extends Entity implements Targetable {
    private final int maxHp;
    private int currentHp;
    private final int speed;
    private final List<Attack> attacks;

    protected Battler(int id, String name, int maxHp, int currentHp, int speed,
                      String spritePath, List<Attack> attacks) {
        super(id, name, spritePath);
        this.maxHp = maxHp;
        this.currentHp = Math.max(0, Math.min(currentHp, maxHp));
        this.speed = speed;
        this.attacks = new ArrayList<>(attacks == null ? List.of() : attacks);
    }

    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public int getCurrentHp() {
        return currentHp;
    }

    public int getSpeed() {
        return speed;
    }

    public List<Attack> getAttacks() {
        return List.copyOf(attacks);
    }

    @Override
    public boolean isAlive() {
        return currentHp > 0;
    }

    @Override
    public void receiveDamage(int amount) {
        currentHp = Math.max(0, currentHp - Math.max(0, amount));
    }

    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + Math.max(0, amount));
    }
}
