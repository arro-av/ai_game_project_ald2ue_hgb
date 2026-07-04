package at.fhooe.ald.view.render;

import java.util.List;
import javafx.geometry.Rectangle2D;

public class BattleRenderResult {
    private final List<Rectangle2D> attackAreas;
    private final List<Rectangle2D> targetAreas;

    public BattleRenderResult(List<Rectangle2D> attackAreas, List<Rectangle2D> targetAreas) {
        this.attackAreas = List.copyOf(attackAreas);
        this.targetAreas = List.copyOf(targetAreas);
    }

    public List<Rectangle2D> getAttackAreas() {
        return attackAreas;
    }

    public List<Rectangle2D> getTargetAreas() {
        return targetAreas;
    }
}
