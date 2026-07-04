package at.fhooe.ald.model;

import java.util.List;

public class PlayerCharacter extends Battler {
    private final String portraitPath;
    private final int joinFloor;

    public PlayerCharacter(int id, String name, int maxHp, int currentHp, int speed,
                           String spritePath, String portraitPath, int joinFloor, List<Attack> attacks) {
        super(id, name, maxHp, currentHp, speed, spritePath, attacks);
        this.portraitPath = portraitPath == null ? "" : portraitPath;
        this.joinFloor = joinFloor;
    }

    public String getPortraitPath() {
        return portraitPath;
    }

    public int getJoinFloor() {
        return joinFloor;
    }
}
