package at.fhooe.ald.view.render;

import at.fhooe.ald.model.Attack;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.Battler;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.service.BattleService;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class BattleRenderer {
    private final SpriteLoader spriteLoader;
    private final HudRenderer hudRenderer;
    private final DialogueBoxRenderer dialogueBoxRenderer;

    public BattleRenderer(SpriteLoader spriteLoader, HudRenderer hudRenderer, DialogueBoxRenderer dialogueBoxRenderer) {
        this.spriteLoader = spriteLoader;
        this.hudRenderer = hudRenderer;
        this.dialogueBoxRenderer = dialogueBoxRenderer;
    }

    public BattleRenderResult render(GraphicsContext graphics, Battle battle, PlayerCharacter activeCharacter,
                                     Attack selectedAttack, List<? extends Targetable> legalTargets,
                                     List<Battler> turnPreview, BattleService battleService, String statusMessage,
                                     double width, double height) {
        drawBackground(graphics, width, height);
        drawFloorHeader(graphics, battle, width);
        drawTurnPreview(graphics, turnPreview);
        drawParty(graphics, battle.getParty());
        drawEnemies(graphics, battle.getEnemies(), width);
        drawBottomPartyHud(graphics, battle.getParty(), height);
        drawBottomEnemyHud(graphics, battle.getEnemies(), width, height);
        List<Rectangle2D> attackAreas = drawAttackMenu(graphics, activeCharacter, battleService);
        List<Rectangle2D> targetAreas = drawTargetMenu(graphics, selectedAttack, legalTargets, width);
        dialogueBoxRenderer.render(graphics, battle, width, height, statusMessage);
        return new BattleRenderResult(attackAreas, targetAreas);
    }

    private void drawBackground(GraphicsContext graphics, double width, double height) {
        graphics.setFill(Color.rgb(42, 46, 50));
        graphics.fillRect(0, 0, width, height);
        graphics.setFill(Color.rgb(70, 66, 58));
        graphics.fillRect(0, height * 0.58, width, height * 0.22);
        graphics.setFill(Color.rgb(30, 32, 36));
        graphics.fillRect(0, height * 0.80, width, height * 0.20);
    }

    private void drawFloorHeader(GraphicsContext graphics, Battle battle, double width) {
        double boxWidth = 270;
        double x = width - boxWidth - 16;
        graphics.setFill(Color.rgb(18, 20, 24));
        graphics.fillRect(x, 14, boxWidth, 38);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.strokeRect(x, 14, boxWidth, 38);
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.setFont(Font.font("Consolas", 16));
        graphics.fillText("Floor " + battle.getFloorNumber() + " - " + battle.getFloorName(), x + 12, 38);
    }

    private void drawParty(GraphicsContext graphics, List<PlayerCharacter> party) {
        double startX = 90;
        double baseY = 285;
        for (int i = 0; i < party.size(); i++) {
            PlayerCharacter character = party.get(i);
            double x = startX + i * 135;
            drawBattler(graphics, character, x, baseY, 92, 92, Color.rgb(76, 150, 210));
            hudRenderer.drawHpBar(graphics, character, x - 10, baseY + 102);
        }
    }

    private void drawEnemies(GraphicsContext graphics, List<Enemy> enemies, double width) {
        double startX = width - 210;
        double baseY = 250;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            double x = startX - i * 135;
            double size = enemy.getEnemyType().name().equals("BOSS") ? 128 : 86;
            drawBattler(graphics, enemy, x, baseY + (128 - size), size, size, Color.rgb(190, 90, 75));
            hudRenderer.drawHpBar(graphics, enemy, x, baseY + 138);
        }
    }

    private void drawBattler(GraphicsContext graphics, Battler battler, double x, double y,
                             double width, double height, Color placeholderColor) {
        Image image = spriteLoader.load(battler.getSpritePath());
        if (image != null && !image.isError()) {
            graphics.drawImage(image, x, y, width, height);
            return;
        }
        graphics.setFill(placeholderColor);
        graphics.fillRect(x, y, width, height);
        graphics.setStroke(Color.rgb(238, 238, 230));
        graphics.strokeRect(x, y, width, height);
        graphics.setFill(Color.rgb(20, 22, 26));
        graphics.setFont(Font.font("Consolas", 13));
        graphics.fillText(battler.getName(), x + 8, y + height / 2);
    }

    private List<Rectangle2D> drawAttackMenu(GraphicsContext graphics, PlayerCharacter activeCharacter,
                                             BattleService battleService) {
        List<Rectangle2D> areas = new ArrayList<>();
        double x = 150;
        double y = 66;
        double width = 300;
        double height = 46;
        graphics.setFont(Font.font("Consolas", 15));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText("Active: " + activeCharacter.getName(), x, y - 14);
        for (int i = 0; i < activeCharacter.getAttacks().size(); i++) {
            Attack attack = activeCharacter.getAttacks().get(i);
            double itemY = y + i * (height + 8);
            Rectangle2D area = new Rectangle2D(x, itemY, width, height);
            areas.add(area);
            boolean ready = battleService.isReady(activeCharacter, attack);
            graphics.setFill(ready ? Color.rgb(32, 36, 42) : Color.rgb(44, 44, 44));
            graphics.fillRect(x, itemY, width, height);
            graphics.setStroke(ready ? Color.rgb(222, 209, 165) : Color.rgb(120, 120, 120));
            graphics.strokeRect(x, itemY, width, height);
            graphics.setFill(ready ? Color.rgb(242, 238, 220) : Color.rgb(155, 155, 150));
            String cooldown = ready ? "" : " CD " + battleService.getRemainingCooldown(activeCharacter, attack);
            graphics.fillText((i + 1) + ". " + attack.getName() + cooldown, x + 12, itemY + 28);
        }
        return areas;
    }

    private List<Rectangle2D> drawTargetMenu(GraphicsContext graphics, Attack selectedAttack,
                                             List<? extends Targetable> legalTargets, double width) {
        List<Rectangle2D> areas = new ArrayList<>();
        if (selectedAttack == null || legalTargets == null || legalTargets.isEmpty()) {
            return areas;
        }
        double x = width - 326;
        double y = 66;
        double itemWidth = 300;
        double itemHeight = 42;
        graphics.setFont(Font.font("Consolas", 15));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText("Targets for " + selectedAttack.getName(), x, y - 14);
        for (int i = 0; i < legalTargets.size(); i++) {
            Targetable target = legalTargets.get(i);
            double itemY = y + i * (itemHeight + 8);
            Rectangle2D area = new Rectangle2D(x, itemY, itemWidth, itemHeight);
            areas.add(area);
            graphics.setFill(Color.rgb(32, 36, 42));
            graphics.fillRect(x, itemY, itemWidth, itemHeight);
            graphics.setStroke(Color.rgb(222, 209, 165));
            graphics.strokeRect(x, itemY, itemWidth, itemHeight);
            graphics.setFill(Color.rgb(242, 238, 220));
            graphics.fillText((i + 1) + ". " + targetName(target) + " HP " + target.getCurrentHp(),
                    x + 12, itemY + 26);
        }
        return areas;
    }

    private void drawTurnPreview(GraphicsContext graphics, List<Battler> turnPreview) {
        double x = 18;
        double y = 66;
        double size = 42;
        graphics.setFont(Font.font("Consolas", 14));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText("Next", x, y - 14);
        for (int i = 0; i < turnPreview.size(); i++) {
            Battler battler = turnPreview.get(i);
            double itemY = y + i * 52;
            graphics.setFill(Color.rgb(18, 20, 24));
            graphics.fillRect(x, itemY, 96, 44);
            graphics.setStroke(i == 0 ? Color.rgb(222, 209, 165) : Color.rgb(90, 94, 102));
            graphics.strokeRect(x, itemY, 96, 44);
            drawPortrait(graphics, battler, x + 3, itemY + 3, size, size);
            graphics.setFill(Color.rgb(242, 238, 220));
            graphics.setFont(Font.font("Consolas", 11));
            graphics.fillText(shortName(battler.getName()), x + 50, itemY + 25);
        }
    }

    private void drawBottomPartyHud(GraphicsContext graphics, List<PlayerCharacter> party, double height) {
        double x = 16;
        double y = height - 128;
        graphics.setFill(Color.rgb(18, 20, 24));
        graphics.fillRect(x, y, 226, 116);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.strokeRect(x, y, 226, 116);
        for (int i = 0; i < party.size(); i++) {
            PlayerCharacter character = party.get(i);
            double rowY = y + 10 + i * 34;
            drawPortrait(graphics, character, x + 8, rowY, 28, 28);
            drawCompactBattlerLine(graphics, character, x + 44, rowY + 5, 160);
        }
    }

    private void drawBottomEnemyHud(GraphicsContext graphics, List<Enemy> enemies, double width, double height) {
        double panelWidth = 226;
        double x = width - panelWidth - 16;
        double y = height - 128;
        graphics.setFill(Color.rgb(18, 20, 24));
        graphics.fillRect(x, y, panelWidth, 116);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.strokeRect(x, y, panelWidth, 116);
        List<Enemy> aliveEnemies = enemies.stream().filter(Enemy::isAlive).limit(3).toList();
        for (int i = 0; i < aliveEnemies.size(); i++) {
            Enemy enemy = aliveEnemies.get(i);
            double rowY = y + 10 + i * 34;
            drawPortrait(graphics, enemy, x + 8, rowY, 28, 28);
            drawCompactBattlerLine(graphics, enemy, x + 44, rowY + 5, 160);
        }
    }

    private void drawPortrait(GraphicsContext graphics, Battler battler, double x, double y, double width,
                              double height) {
        String path = battler instanceof PlayerCharacter playerCharacter
                ? playerCharacter.getPortraitPath()
                : battler.getSpritePath();
        Image image = spriteLoader.load(path);
        if (image != null && !image.isError()) {
            graphics.drawImage(image, x, y, width, height);
            return;
        }
        graphics.setFill(battler instanceof Enemy ? Color.rgb(190, 90, 75) : Color.rgb(76, 150, 210));
        graphics.fillRect(x, y, width, height);
        graphics.setStroke(Color.rgb(238, 238, 230));
        graphics.strokeRect(x, y, width, height);
    }

    private void drawCompactBattlerLine(GraphicsContext graphics, Battler battler, double x, double y,
                                        double width) {
        graphics.setFont(Font.font("Consolas", 12));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText(shortName(battler.getName()) + " " + battler.getCurrentHp() + "/" + battler.getMaxHp(),
                x, y + 8);
        double ratio = battler.getMaxHp() == 0 ? 0 : (double) battler.getCurrentHp() / battler.getMaxHp();
        graphics.setFill(Color.rgb(28, 30, 35));
        graphics.fillRect(x, y + 14, width, 8);
        graphics.setFill(ratio > 0.35 ? Color.rgb(68, 190, 108) : Color.rgb(210, 72, 70));
        graphics.fillRect(x, y + 14, width * Math.max(0, ratio), 8);
        graphics.setStroke(Color.rgb(230, 230, 220));
        graphics.strokeRect(x, y + 14, width, 8);
    }

    private String targetName(Targetable target) {
        return target instanceof Battler battler ? battler.getName() : "Target";
    }

    private String shortName(String name) {
        return name.length() <= 9 ? name : name.substring(0, 9);
    }
}
