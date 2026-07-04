package at.fhooe.ald.model;

import java.util.Objects;

public class Attack {
    private final int id;
    private final String name;
    private final String description;
    private final int minDamage;
    private final int maxDamage;
    private final TargetType targetType;
    private final AttackEffect effect;
    private final int cooldown;
    private final String unlockState;

    public Attack(int id, String name, String description, int minDamage, int maxDamage,
                  TargetType targetType, AttackEffect effect, int cooldown, String unlockState) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNullElse(description, "");
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.targetType = Objects.requireNonNull(targetType);
        this.effect = Objects.requireNonNull(effect);
        this.cooldown = cooldown;
        this.unlockState = unlockState;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMinDamage() {
        return minDamage;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public AttackEffect getEffect() {
        return effect;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getUnlockState() {
        return unlockState;
    }
}
