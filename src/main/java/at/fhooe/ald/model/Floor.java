package at.fhooe.ald.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Floor {
    private final int id;
    private final int floorNumber;
    private final String name;
    private final String backgroundPath;
    private final List<Enemy> enemies;

    public Floor(int id, int floorNumber, String name, String backgroundPath, List<Enemy> enemies) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.name = Objects.requireNonNull(name);
        this.backgroundPath = Objects.requireNonNullElse(backgroundPath, "");
        this.enemies = new ArrayList<>(enemies == null ? List.of() : enemies);
    }

    public int getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public String getName() {
        return name;
    }

    public String getBackgroundPath() {
        return backgroundPath;
    }

    public List<Enemy> getEnemies() {
        return List.copyOf(enemies);
    }
}
