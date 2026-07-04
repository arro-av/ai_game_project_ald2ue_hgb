package at.fhooe.ald.model;

public interface TurnActor {
    boolean canAct();

    Attack chooseAction(Battle battle);
}
