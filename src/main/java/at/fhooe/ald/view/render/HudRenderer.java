package at.fhooe.ald.view.render;

import at.fhooe.ald.model.Battler;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class HudRenderer {
    private static final double BAR_WIDTH = 120;
    private static final double BAR_HEIGHT = 10;
    private static final double HP_EASING = 0.16;

    private final Map<Battler, Double> displayedHpByBattler = new IdentityHashMap<>();

    public void drawHpBar(GraphicsContext graphics, Battler battler, double x, double y) {
        double displayedHp = updateDisplayedHp(battler);
        double hpRatio = battler.getMaxHp() == 0 ? 0 : displayedHp / battler.getMaxHp();
        graphics.setFill(Color.rgb(24, 26, 30));
        graphics.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);
        graphics.setFill(hpRatio > 0.35 ? Color.rgb(68, 190, 108) : Color.rgb(210, 72, 70));
        graphics.fillRect(x, y, BAR_WIDTH * Math.max(0, hpRatio), BAR_HEIGHT);
        graphics.setStroke(Color.rgb(230, 230, 220));
        graphics.strokeRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        graphics.setFont(Font.font("Consolas", 12));
        graphics.setFill(Color.rgb(238, 238, 230));
        graphics.fillText(Math.round(displayedHp) + "/" + battler.getMaxHp(), x + 4, y - 4);
    }

    private double updateDisplayedHp(Battler battler) {
        double currentHp = battler.getCurrentHp();
        double displayedHp = displayedHpByBattler.getOrDefault(battler, currentHp);
        double difference = currentHp - displayedHp;
        if (Math.abs(difference) < 0.5) {
            displayedHp = currentHp;
        } else {
            displayedHp += difference * HP_EASING;
        }
        displayedHpByBattler.put(battler, displayedHp);
        return Math.max(0, Math.min(battler.getMaxHp(), displayedHp));
    }
}
