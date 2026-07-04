package at.fhooe.ald.model;

import java.util.Objects;

public abstract class Entity {
    private final int id;
    private final String name;
    private final String spritePath;

    protected Entity(int id, String name, String spritePath) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.spritePath = Objects.requireNonNullElse(spritePath, "");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpritePath() {
        return spritePath;
    }
}
