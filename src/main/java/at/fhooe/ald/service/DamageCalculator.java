package at.fhooe.ald.service;

import at.fhooe.ald.model.Attack;
import java.util.Objects;
import java.util.Random;

public class DamageCalculator {
    private final Random random;

    public DamageCalculator() {
        this(new Random());
    }

    public DamageCalculator(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public int rollDamage(Attack attack) {
        int minDamage = Math.max(0, attack.getMinDamage());
        int maxDamage = Math.max(minDamage, attack.getMaxDamage());
        if (maxDamage == minDamage) {
            return minDamage;
        }
        return random.nextInt(maxDamage - minDamage + 1) + minDamage;
    }
}
