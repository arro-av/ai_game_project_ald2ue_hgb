package at.fhooe.ald.view.render;

import at.fhooe.ald.model.Battler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class HudRenderer {
    private static final double BAR_WIDTH = 120;
    private static final double BAR_HEIGHT = 10;

    public void drawHpBar(GraphicsContext graphics, Battler battler, double x, double y) {
        double hpRatio = battler.getMaxHp() == 0 ? 0 : (double) battler.getCurrentHp() / battler.getMaxHp();
        graphics.setFill(Color.rgb(24, 26, 30));
        graphics.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);
        graphics.setFill(hpRatio > 0.35 ? Color.rgb(68, 190, 108) : Color.rgb(210, 72, 70));
        graphics.fillRect(x, y, BAR_WIDTH * Math.max(0, hpRatio), BAR_HEIGHT);
        graphics.setStroke(Color.rgb(230, 230, 220));
        graphics.strokeRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        graphics.setFont(Font.font("Consolas", 12));
        graphics.setFill(Color.rgb(238, 238, 230));
        graphics.fillText(battler.getCurrentHp() + "/" + battler.getMaxHp(), x + 4, y - 4);
    }
}
