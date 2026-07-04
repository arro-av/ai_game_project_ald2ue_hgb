package at.fhooe.ald.model;

import java.util.Objects;

public class DialogueLine {
    private final int id;
    private final int floorNumber;
    private final String speaker;
    private final String text;
    private final int displayOrder;

    public DialogueLine(int id, int floorNumber, String speaker, String text, int displayOrder) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.speaker = Objects.requireNonNullElse(speaker, "");
        this.text = Objects.requireNonNull(text);
        this.displayOrder = displayOrder;
    }

    public int getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
