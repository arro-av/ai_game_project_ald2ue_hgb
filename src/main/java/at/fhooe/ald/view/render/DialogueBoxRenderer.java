package at.fhooe.ald.view.render;

import at.fhooe.ald.model.Battle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class DialogueBoxRenderer {

    public void render(GraphicsContext graphics, Battle battle, double width, double height, String statusMessage) {
        double boxHeight = 116;
        double y = height - boxHeight - 12;
        double boxWidth = Math.min(460, width - 520);
        double x = (width - boxWidth) / 2;
        graphics.setFill(Color.rgb(18, 20, 24));
        graphics.fillRect(x, y, boxWidth, boxHeight);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.setLineWidth(2);
        graphics.strokeRect(x, y, boxWidth, boxHeight);

        graphics.setFont(Font.font("Consolas", 16));
        graphics.setFill(Color.rgb(242, 238, 220));
        String text = latestMessage(battle, statusMessage);
        drawWrapped(graphics, text, x + 18, y + 30, boxWidth - 36, 20, 4);
    }

    private String latestMessage(Battle battle, String statusMessage) {
        if (statusMessage != null && !statusMessage.isBlank()) {
            return statusMessage;
        }
        if (!battle.getTurnLog().isEmpty()) {
            return battle.getTurnLog().getLast().getMessage();
        }
        if (!battle.getIntroDialogue().isEmpty()) {
            var line = battle.getIntroDialogue().getFirst();
            return line.getSpeaker().isBlank() ? line.getText() : line.getSpeaker() + ": " + line.getText();
        }
        return "Choose an attack.";
    }

    private void drawWrapped(GraphicsContext graphics, String text, double x, double y,
                             double maxWidth, double lineHeight, int maxLines) {
        String[] words = text.split("\\s+");
        String line = "";
        int lines = 0;
        for (String word : words) {
            String candidate = line.isBlank() ? word : line + " " + word;
            if (graphics.getFont().getSize() * candidate.length() * 0.58 > maxWidth && !line.isBlank()) {
                graphics.fillText(line, x, y + lines * lineHeight);
                lines++;
                line = word;
                if (lines >= maxLines) {
                    return;
                }
            } else {
                line = candidate;
            }
        }
        if (!line.isBlank() && lines < maxLines) {
            graphics.fillText(line, x, y + lines * lineHeight);
        }
    }
}
