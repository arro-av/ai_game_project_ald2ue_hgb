package at.fhooe.ald.model;

public interface Targetable {
    boolean isAlive();

    int getCurrentHp();

    void receiveDamage(int amount);
}
