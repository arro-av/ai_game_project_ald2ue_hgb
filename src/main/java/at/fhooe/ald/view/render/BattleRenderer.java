package at.fhooe.ald.view.render;

import at.fhooe.ald.model.Attack;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.BattleTurn;
import at.fhooe.ald.model.Battler;
import at.fhooe.ald.model.BossEnemy;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.service.BattleService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class BattleRenderer {
    private static final double DEATH_FALL_SPEED = 10.0;
    private static final double PARTY_SPRITE_SIZE = 300.0;
    private static final double BOSS_SPRITE_SIZE = 417.0;
    private static final double TRASH_SPRITE_SIZE = 280.5;
    private static final double PARTY_SPACING = 170.0;
    private static final double ENEMY_SPACING = 185.0;
    private static final double PANEL_MARGIN = 18.0;
    private static final double BOTTOM_PANEL_HEIGHT = 132.0;
    private static final double SPRITE_PANEL_GAP = 34.0;
    private static final double HP_BAR_WIDTH = 120.0;
    private static final double STATUS_ICON_SIZE = 22.0;
    private static final double STATUS_ICON_GAP = 4.0;
    private static final double ENEMY_STACK_Y_OFFSET = 146.0;
    private static final double PROJECTILE_EFFECT_SIZE = 288.0;
    private static final double MELEE_EFFECT_SIZE = 384.0;
    private static final long EFFECT_ANIMATION_NANOS = 450_000_000L;
    private static final long HIT_FLASH_NANOS = 750_000_000L;
    private static final int HIT_FLASH_COUNT = 3;
    private static final double TURN_PREVIEW_TOP = 10.0;
    private static final double TURN_PREVIEW_ACTIVE_SIZE = 45.0;
    private static final double TURN_PREVIEW_NEXT_SIZE = 39.0;
    private static final double TURN_PREVIEW_GAP = 9.0;
    private static final String ARMOR_ICON = "/assets/ui/icons/armor.png";
    private static final String BLEED_ICON = "/assets/ui/icons/bleed.png";
    private static final String BURN_ICON = "/assets/ui/icons/burn.png";
    private static final String CHARM_ICON = "/assets/ui/icons/charm.png";
    private static final String DAMAGE_ICON = "/assets/ui/icons/damage.png";
    private static final String HEAL_ICON = "/assets/ui/icons/heal.png";
    private static final String POISON_ICON = "/assets/ui/icons/poison.png";
    private static final String PROTECTION_ICON = "/assets/ui/icons/protection.png";
    private static final String REFLECT_ICON = "/assets/ui/icons/reflect.png";
    private static final String SKULL_ICON = "/assets/ui/icons/skull.png";
    private static final String STUN_ICON = "/assets/ui/icons/stun.png";
    private static final Map<String, String> ATTACK_EFFECT_SPRITES = Map.ofEntries(
            Map.entry("Bug Bite", "/assets/sprites/effects/bug_bite.png"),
            Map.entry("Explosive Toss", "/assets/sprites/effects/explosive_toss.png"),
            Map.entry("Fireball", "/assets/sprites/effects/fireball.png"),
            Map.entry("Gut Ripper", "/assets/sprites/effects/gut_ripper.png"),
            Map.entry("Magic Missile", "/assets/sprites/effects/magic_missile.png"),
            Map.entry("Pile Collapse", "/assets/sprites/effects/pile_collapse.png"),
            Map.entry("Rake", "/assets/sprites/effects/rake.png"),
            Map.entry("Raptor Bite", "/assets/sprites/effects/raptor_bite.png"),
            Map.entry("Rodent Bite", "/assets/sprites/effects/rodent_bite.png"),
            Map.entry("Roundhouse Kick", "/assets/sprites/effects/roundhouse_kick.png")
    );
    private static final Map<String, String> TURN_PREVIEW_PORTRAITS = Map.ofEntries(
            Map.entry("carl", "/assets/ui/portraits/portrait_carl.png"),
            Map.entry("donut", "/assets/ui/portraits/portrait_donut.png"),
            Map.entry("mongo", "/assets/ui/portraits/portrait_mongo.png"),
            Map.entry("hoarder", "/assets/ui/portraits/portrait_hoarder.png"),
            Map.entry("scatterer", "/assets/ui/portraits/portrait_scatterer.png"),
            Map.entry("ralph", "/assets/sprites/bosses/ralph_idle.png"),
            Map.entry("heather", "/assets/ui/portraits/portrait_heather.png"),
            Map.entry("gore-gore", "/assets/ui/portraits/portrait_goregore.png"),
            Map.entry("denise", "/assets/ui/portraits/portrait_denise.png"),
            Map.entry("circe", "/assets/ui/portraits/portrait_circe.png"),
            Map.entry("mantis nymph", "/assets/ui/portraits/portrait_nymph.png")
    );
    private static final Map<String, SpriteLayout> SPRITE_LAYOUTS = Map.ofEntries(
            Map.entry("carl", new SpriteLayout(-10, -40, 280, 280)),
            Map.entry("donut", new SpriteLayout(-20, 0, 240, 240)),
            Map.entry("mongo", new SpriteLayout(-80, -70, 260, 260)),
            Map.entry("hoarder", new SpriteLayout(0, -40, 450, 450)),
            Map.entry("scatterer", new SpriteLayout(0, 0, 220, 220)),
            Map.entry("ralph", new SpriteLayout(0, 0, 417, 417)),
            Map.entry("heather", new SpriteLayout(0, 0, 417, 417)),
            Map.entry("gore-gore", new SpriteLayout(0, 0, 417, 417)),
            Map.entry("denise", new SpriteLayout(0, 0, 417, 417)),
            Map.entry("circe", new SpriteLayout(0, 0, 417, 417)),
            Map.entry("mantis nymph", new SpriteLayout(0, 0, 280.5, 280.5))
    );

    private final SpriteLoader spriteLoader;
    private final HudRenderer hudRenderer;
    private final Map<Battler, Double> deathOffsets;
    private final Map<Battler, Boolean> despawnedBattlers;
    private final Map<String, Long> hitFlashStartsByName;
    private final List<EffectAnimation> effectAnimations;
    private int animatedTurnCount;

    public BattleRenderer(SpriteLoader spriteLoader, HudRenderer hudRenderer) {
        this.spriteLoader = spriteLoader;
        this.hudRenderer = hudRenderer;
        this.deathOffsets = new IdentityHashMap<>();
        this.despawnedBattlers = new IdentityHashMap<>();
        this.hitFlashStartsByName = new HashMap<>();
        this.effectAnimations = new ArrayList<>();
        this.animatedTurnCount = 0;
    }

    public BattleRenderResult render(GraphicsContext graphics, Battle battle, PlayerCharacter activeCharacter,
                                     Attack selectedAttack, List<? extends Targetable> legalTargets,
                                     BattleService battleService, String statusMessage, boolean attacksEnabled,
                                     double hoverX, double hoverY, double width, double height) {
        drawBackground(graphics, battle, width, height);
        drawTurnPreview(graphics, battleService.getTurnPreview(battle, 5), width);
        Map<Targetable, Rectangle2D> targetAreaByTarget = new IdentityHashMap<>();
        Map<String, List<Rectangle2D>> battlerAreasByName = new HashMap<>();
        drawParty(graphics, battle, battleService, legalTargets, targetAreaByTarget, battlerAreasByName, height);
        drawEnemies(graphics, battle, battleService, legalTargets, targetAreaByTarget, battlerAreasByName,
                width, height);
        startAnimationsForNewTurns(battle, battlerAreasByName);
        drawEffectAnimations(graphics);
        drawHoveredTargetBox(graphics, targetAreaByTarget, legalTargets, hoverX, hoverY);
        List<Rectangle2D> attackAreas = drawActiveCharacterPanel(graphics, activeCharacter, battleService,
                attacksEnabled, height);
        drawLogPanel(graphics, battle, statusMessage, height);
        drawAttackDescriptionPanel(graphics, selectedAttack, width, height);
        List<Rectangle2D> targetAreas = legalTargets.stream()
                .map(targetAreaByTarget::get)
                .filter(area -> area != null)
                .toList();
        return new BattleRenderResult(attackAreas, targetAreas);
    }

    private void drawTurnPreview(GraphicsContext graphics, List<Battler> turnPreview, double canvasWidth) {
        if (turnPreview.isEmpty()) {
            return;
        }
        double totalWidth = TURN_PREVIEW_ACTIVE_SIZE
                + Math.max(0, turnPreview.size() - 1) * (TURN_PREVIEW_NEXT_SIZE + TURN_PREVIEW_GAP);
        double x = canvasWidth / 2 - totalWidth / 2;
        double centerY = TURN_PREVIEW_TOP + TURN_PREVIEW_ACTIVE_SIZE / 2;

        double panelPadding = 6;
        graphics.setFill(Color.rgb(16, 18, 22, 0.58));
        graphics.fillRect(x - panelPadding, TURN_PREVIEW_TOP - panelPadding,
                totalWidth + panelPadding * 2, TURN_PREVIEW_ACTIVE_SIZE + panelPadding * 2);
        graphics.setStroke(Color.rgb(238, 226, 178, 0.35));
        graphics.strokeRect(x - panelPadding, TURN_PREVIEW_TOP - panelPadding,
                totalWidth + panelPadding * 2, TURN_PREVIEW_ACTIVE_SIZE + panelPadding * 2);

        for (int i = 0; i < turnPreview.size(); i++) {
            Battler battler = turnPreview.get(i);
            boolean active = i == 0;
            double size = active ? TURN_PREVIEW_ACTIVE_SIZE : TURN_PREVIEW_NEXT_SIZE;
            double y = centerY - size / 2;
            drawTurnPreviewPortrait(graphics, battler, x, y, size, active);
            x += size + TURN_PREVIEW_GAP;
        }
    }

    private void drawTurnPreviewPortrait(GraphicsContext graphics, Battler battler, double x, double y,
                                         double size, boolean active) {
        graphics.save();
        graphics.setGlobalAlpha(active ? 1.0 : 0.5);
        Image image = spriteLoader.load(portraitPathFor(battler));
        if (image != null && !image.isError()) {
            graphics.drawImage(image, x, y, size, size);
        } else {
            graphics.setFill(battler instanceof Enemy ? Color.rgb(190, 90, 75) : Color.rgb(76, 150, 210));
            graphics.fillRect(x, y, size, size);
        }
        graphics.restore();

        graphics.setLineWidth(active ? 3 : 2);
        graphics.setStroke(active ? Color.rgb(255, 226, 122) : Color.rgb(220, 220, 210, 0.55));
        graphics.strokeRect(x - 2, y - 2, size + 4, size + 4);
        if (active) {
            graphics.setStroke(Color.rgb(255, 255, 235, 0.85));
            graphics.strokeRect(x - 6, y - 6, size + 12, size + 12);
        }
    }

    private void drawBackground(GraphicsContext graphics, Battle battle, double width, double height) {
        Image background = spriteLoader.load(battle.getBackgroundPath());
        if (background != null && !background.isError()) {
            graphics.drawImage(background, 0, 0, width, height);
            graphics.setFill(Color.rgb(8, 10, 14, 0.24));
            graphics.fillRect(0, 0, width, height);
            graphics.setFill(Color.rgb(8, 10, 14, 0.46));
            graphics.fillRect(0, height * 0.80, width, height * 0.20);
            return;
        }
        graphics.setFill(Color.rgb(42, 46, 50));
        graphics.fillRect(0, 0, width, height);
        graphics.setFill(Color.rgb(70, 66, 58));
        graphics.fillRect(0, height * 0.58, width, height * 0.22);
        graphics.setFill(Color.rgb(30, 32, 36));
        graphics.fillRect(0, height * 0.80, width, height * 0.20);
    }

    private void drawParty(GraphicsContext graphics, Battle battle, BattleService battleService,
                           List<? extends Targetable> legalTargets, Map<Targetable, Rectangle2D> targetAreaByTarget,
                           Map<String, List<Rectangle2D>> battlerAreasByName, double canvasHeight) {
        double spriteBottomY = battleSpriteBottomY(canvasHeight);
        double startX = 80;
        double targetAlpha = targetBlinkAlpha();
        List<PlayerCharacter> party = battle.getParty();
        for (int i = 0; i < party.size(); i++) {
            PlayerCharacter character = party.get(i);
            if (isDespawned(character)) {
                continue;
            }
            SpriteLayout layout = spriteLayout(character, PARTY_SPRITE_SIZE);
            double x = startX + i * PARTY_SPACING + layout.xOffset();
            double y = spriteBottomY - layout.height() + layout.yOffset();
            double deathOffset = updateDeathOffset(character, y, layout.height(), graphics.getCanvas().getHeight());
            boolean legalTarget = isLegalTarget(character, legalTargets);
            double hitAlpha = hitFlashAlpha(character.getName());
            drawBattler(graphics, character, x, y + deathOffset, layout.width(), layout.height(),
                    Color.rgb(76, 150, 210), (legalTarget ? targetAlpha : 1.0) * hitAlpha);
            Rectangle2D area = new Rectangle2D(x, y + deathOffset, layout.width(), layout.height());
            battlerAreasByName.computeIfAbsent(character.getName(), ignored -> new ArrayList<>()).add(area);
            if (legalTarget) {
                targetAreaByTarget.put(character, area);
            }
            double hpBarX = x + Math.max(0, (layout.width() - HP_BAR_WIDTH) / 2);
            double hpBarY = y + layout.height() + 12 + deathOffset;
            hudRenderer.drawHpBar(graphics, character, hpBarX, hpBarY);
            drawStatusIcons(graphics, battle, battleService, character, hpBarX + HP_BAR_WIDTH + 8, hpBarY - 7);
        }
    }

    private void drawEnemies(GraphicsContext graphics, Battle battle, BattleService battleService,
                             List<? extends Targetable> legalTargets, Map<Targetable, Rectangle2D> targetAreaByTarget,
                             Map<String, List<Rectangle2D>> battlerAreasByName, double width, double height) {
        double spriteBottomY = battleSpriteBottomY(height);
        double startX = width - 470;
        double targetAlpha = targetBlinkAlpha();
        int visibleIndex = 0;
        Map<String, List<Integer>> stackColumnsByName = new HashMap<>();
        Map<String, Integer> occurrenceByName = new HashMap<>();
        List<Enemy> enemies = battle.getEnemies();
        for (Enemy enemy : enemies) {
            if (isDespawned(enemy)) {
                continue;
            }
            double defaultSize = enemy.getEnemyType().name().equals("BOSS") ? BOSS_SPRITE_SIZE : TRASH_SPRITE_SIZE;
            SpriteLayout layout = spriteLayout(enemy, defaultSize);
            EnemyStackPosition stackPosition = enemyStackPosition(enemy, visibleIndex, stackColumnsByName,
                    occurrenceByName);
            visibleIndex = stackPosition.nextVisibleIndex();
            double x = startX - stackPosition.columnIndex() * ENEMY_SPACING + layout.xOffset();
            double y = spriteBottomY - layout.height() + layout.yOffset()
                    - stackPosition.stackIndex() * ENEMY_STACK_Y_OFFSET;
            double deathOffset = updateDeathOffset(enemy, y, layout.height(), graphics.getCanvas().getHeight());
            boolean legalTarget = isLegalTarget(enemy, legalTargets);
            double hitAlpha = hitFlashAlpha(enemy.getName());
            drawBattler(graphics, enemy, x, y + deathOffset, layout.width(), layout.height(),
                    Color.rgb(190, 90, 75), (legalTarget ? targetAlpha : 1.0) * hitAlpha);
            Rectangle2D area = new Rectangle2D(x, y + deathOffset, layout.width(), layout.height());
            battlerAreasByName.computeIfAbsent(enemy.getName(), ignored -> new ArrayList<>()).add(area);
            double hpBarX = x + Math.max(0, (layout.width() - HP_BAR_WIDTH) / 2);
            double hpBarY = y + layout.height() + 12 + deathOffset;
            hudRenderer.drawHpBar(graphics, enemy, hpBarX, hpBarY);
            drawStatusIcons(graphics, battle, battleService, enemy, hpBarX + HP_BAR_WIDTH + 8, hpBarY - 7);
            if (legalTarget) {
                targetAreaByTarget.put(enemy, area);
            }
        }
    }

    private EnemyStackPosition enemyStackPosition(Enemy enemy, int visibleIndex,
                                                  Map<String, List<Integer>> stackColumnsByName,
                                                  Map<String, Integer> occurrenceByName) {
        if (!isStackableEnemy(enemy)) {
            return new EnemyStackPosition(visibleIndex, 0, visibleIndex + 1);
        }
        String name = normalizeName(enemy.getName());
        int occurrence = occurrenceByName.merge(name, 1, Integer::sum) - 1;
        List<Integer> columns = stackColumnsByName.computeIfAbsent(name, ignored -> new ArrayList<>());
        if (occurrence < 2 || columns.isEmpty()) {
            columns.add(visibleIndex);
            return new EnemyStackPosition(visibleIndex, 0, visibleIndex + 1);
        }
        int columnIndex = columns.get((occurrence - 2) % columns.size());
        return new EnemyStackPosition(columnIndex, occurrence / 2, visibleIndex);
    }

    private boolean isStackableEnemy(Enemy enemy) {
        String name = normalizeName(enemy.getName());
        return name.equals("scatterer") || name.equals("mantis nymph");
    }

    private void drawStatusIcons(GraphicsContext graphics, Battle battle, BattleService battleService,
                                 Battler battler, double x, double y) {
        if (!battler.isAlive()) {
            return;
        }
        List<String> icons = statusIconsFor(battle, battleService, battler);
        if (icons.isEmpty()) {
            return;
        }
        for (int i = 0; i < icons.size(); i++) {
            Image icon = spriteLoader.load(icons.get(i));
            if (icon != null && !icon.isError()) {
                graphics.drawImage(icon, x + i * (STATUS_ICON_SIZE + STATUS_ICON_GAP), y,
                        STATUS_ICON_SIZE, STATUS_ICON_SIZE);
            }
        }
    }

    private List<String> statusIconsFor(Battle battle, BattleService battleService, Battler battler) {
        List<String> icons = new ArrayList<>();
        if (battler instanceof PlayerCharacter && battleService.hasPartyProtection(battle)) {
            icons.add(PROTECTION_ICON);
        }
        if (battler instanceof PlayerCharacter && battleService.hasPartyDamageBoost(battle)) {
            icons.add(DAMAGE_ICON);
        }
        if (hasArmorIcon(battle, battleService, battler)) {
            icons.add(ARMOR_ICON);
        }
        if (hasReflectIcon(battler)) {
            icons.add(REFLECT_ICON);
        }
        if (battleService.hasRoyalCharm(battle, battler)) {
            icons.add(CHARM_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Stun")) {
            icons.add(STUN_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Burn")) {
            icons.add(BURN_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Bleed")) {
            icons.add(BLEED_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Infection")) {
            icons.add(POISON_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Lethal Infection")) {
            icons.add(SKULL_ICON);
        }
        if (battleService.hasStatusEffect(battle, battler, "Healing Song")) {
            icons.add(HEAL_ICON);
        }
        return icons;
    }

    private boolean hasArmorIcon(Battle battle, BattleService battleService, Battler battler) {
        if (battler instanceof BossEnemy bossEnemy && bossEnemy.getName().equals("Hoarder")) {
            return battle.getAliveEnemies().stream().anyMatch(enemy -> enemy.getName().equals("Scatterer"));
        }
        if (battler instanceof BossEnemy bossEnemy && bossEnemy.getName().equals("Heather")) {
            return bossEnemy.getState().name().equals("FINAL_PHASE");
        }
        if (battler instanceof BossEnemy bossEnemy && bossEnemy.getName().equals("Denise")) {
            return bossEnemy.isAlive();
        }
        return battler instanceof Enemy enemy
                && enemy.getName().equals("Mantis Nymph")
                && battleService.hasNymphDamageBoost(battle);
    }

    private boolean hasReflectIcon(Battler battler) {
        return battler instanceof BossEnemy bossEnemy
                && bossEnemy.getName().equals("Denise")
                && bossEnemy.getState().name().equals("FINAL_PHASE");
    }

    private void drawBattler(GraphicsContext graphics, Battler battler, double x, double y,
                             double width, double height, Color placeholderColor) {
        drawBattler(graphics, battler, x, y, width, height, placeholderColor, 1.0);
    }

    private void drawBattler(GraphicsContext graphics, Battler battler, double x, double y,
                             double width, double height, Color placeholderColor, double alpha) {
        graphics.save();
        graphics.setGlobalAlpha(alpha);
        Image image = spriteLoader.load(spritePathFor(battler));
        if (image != null && !image.isError()) {
            graphics.drawImage(image, x, y, width, height);
            graphics.restore();
            return;
        }
        graphics.setFill(placeholderColor);
        graphics.fillRect(x, y, width, height);
        graphics.setStroke(Color.rgb(238, 238, 230));
        graphics.strokeRect(x, y, width, height);
        graphics.setFill(Color.rgb(20, 22, 26));
        graphics.setFont(Font.font("Consolas", 13));
        graphics.fillText(battler.getName(), x + 8, y + height / 2);
        graphics.restore();
    }

    private String spritePathFor(Battler battler) {
        if (battler instanceof BossEnemy bossEnemy
                && bossEnemy.getName().equals("Circe")
                && bossEnemy.getState().name().equals("FINAL_PHASE")) {
            return "/assets/sprites/bosses/boss_circe_phase3_idle.png";
        }
        return battler.getSpritePath();
    }

    private String portraitPathFor(Battler battler) {
        String previewPortrait = TURN_PREVIEW_PORTRAITS.get(normalizeName(battler.getName()));
        if (previewPortrait != null) {
            return previewPortrait;
        }
        if (battler instanceof PlayerCharacter playerCharacter && !playerCharacter.getPortraitPath().isBlank()) {
            return playerCharacter.getPortraitPath();
        }
        return spritePathFor(battler);
    }

    private List<Rectangle2D> drawActiveCharacterPanel(GraphicsContext graphics, PlayerCharacter activeCharacter,
                                                       BattleService battleService, boolean attacksEnabled,
                                                       double canvasHeight) {
        List<Rectangle2D> areas = new ArrayList<>();
        double x = PANEL_MARGIN;
        double y = panelY(canvasHeight);
        double panelWidth = 380;
        double panelHeight = BOTTOM_PANEL_HEIGHT;
        graphics.setFill(Color.rgb(18, 20, 24, 0.90));
        graphics.fillRect(x, y, panelWidth, panelHeight);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.setLineWidth(2);
        graphics.strokeRect(x, y, panelWidth, panelHeight);

        drawPortrait(graphics, activeCharacter, x + 14, y + 16, 74, 74);
        graphics.setFont(Font.font("Consolas", 14));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText(activeCharacter.getName(), x + 14, y + 114);

        double buttonX = x + 104;
        double buttonY = y + 16;
        double width = 250;
        double height = 30;
        for (int i = 0; i < activeCharacter.getAttacks().size(); i++) {
            Attack attack = activeCharacter.getAttacks().get(i);
            double itemY = buttonY + i * (height + 8);
            Rectangle2D area = new Rectangle2D(buttonX, itemY, width, height);
            areas.add(area);
            int remainingCooldown = battleService.getRemainingCooldown(activeCharacter, attack);
            boolean attackReady = remainingCooldown == 0;
            boolean ready = attacksEnabled && attackReady;
            graphics.setFill(ready ? Color.rgb(32, 36, 42) : Color.rgb(44, 44, 44));
            graphics.fillRect(buttonX, itemY, width, height);
            graphics.setStroke(ready ? Color.rgb(222, 209, 165) : Color.rgb(120, 120, 120));
            graphics.strokeRect(buttonX, itemY, width, height);
            graphics.setFill(ready ? Color.rgb(242, 238, 220) : Color.rgb(155, 155, 150));
            String cooldown = attacksEnabled && remainingCooldown > 0 ? " CD " + remainingCooldown : "";
            graphics.setFont(Font.font("Consolas", 12));
            graphics.fillText((i + 1) + ". " + attack.getName() + cooldown, buttonX + 10, itemY + 20);
        }
        return areas;
    }

    private void drawLogPanel(GraphicsContext graphics, Battle battle, String statusMessage, double height) {
        double panelWidth = 420;
        double panelHeight = BOTTOM_PANEL_HEIGHT;
        double x = (graphics.getCanvas().getWidth() - panelWidth) / 2;
        double y = panelY(height);
        graphics.setFill(Color.rgb(18, 20, 24, 0.90));
        graphics.fillRect(x, y, panelWidth, panelHeight);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.setLineWidth(2);
        graphics.strokeRect(x, y, panelWidth, panelHeight);

        graphics.setFont(Font.font("Consolas", 15));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText("Log", x + 16, y + 26);
        graphics.setFont(Font.font("Consolas", 13));
        graphics.setFill(Color.rgb(216, 200, 148));
        drawWrapped(graphics, latestMessage(battle, statusMessage), x + 16, y + 52, panelWidth - 32, 18, 4);
    }

    private void drawAttackDescriptionPanel(GraphicsContext graphics, Attack selectedAttack, double width,
                                            double height) {
        double panelWidth = 380;
        double panelHeight = BOTTOM_PANEL_HEIGHT;
        double x = width - panelWidth - PANEL_MARGIN;
        double y = panelY(height);
        graphics.setFill(Color.rgb(18, 20, 24, 0.90));
        graphics.fillRect(x, y, panelWidth, panelHeight);
        graphics.setStroke(Color.rgb(222, 209, 165));
        graphics.setLineWidth(2);
        graphics.strokeRect(x, y, panelWidth, panelHeight);

        String title = "Attack";
        String text = "Pick an attack.";
        if (selectedAttack != null) {
            title = selectedAttack.getName();
            text = selectedAttack.getDescription();
        }
        graphics.setFont(Font.font("Consolas", 15));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText(title, x + 16, y + 26);
        graphics.setFont(Font.font("Consolas", 13));
        graphics.setFill(Color.rgb(216, 200, 148));
        drawWrapped(graphics, text, x + 16, y + 52, panelWidth - 32, 18, 4);
    }

    private double panelY(double canvasHeight) {
        return canvasHeight - BOTTOM_PANEL_HEIGHT - PANEL_MARGIN;
    }

    private double battleSpriteBottomY(double canvasHeight) {
        return panelY(canvasHeight) - SPRITE_PANEL_GAP;
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

    private String latestMessage(Battle battle, String statusMessage) {
        if (statusMessage != null) {
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
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines++;
                if (lines >= maxLines) {
                    return;
                }
                continue;
            }
            String[] words = paragraph.split("\\s+");
            String line = "";
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
                lines++;
                if (lines >= maxLines) {
                    return;
                }
            }
        }
    }

    private boolean isLegalTarget(Targetable targetable, List<? extends Targetable> legalTargets) {
        return legalTargets != null && legalTargets.stream().anyMatch(target -> target == targetable);
    }

    private double targetBlinkAlpha() {
        return 0.5 + 0.5 * ((Math.sin(System.currentTimeMillis() / 260.0) + 1.0) / 2.0);
    }

    private double hitFlashAlpha(String battlerName) {
        Long startedAt = hitFlashStartsByName.get(battlerName);
        if (startedAt == null) {
            return 1.0;
        }
        long elapsed = System.nanoTime() - startedAt;
        if (elapsed >= HIT_FLASH_NANOS) {
            hitFlashStartsByName.remove(battlerName);
            return 1.0;
        }
        double progress = elapsed / (double) HIT_FLASH_NANOS;
        int phase = (int) Math.floor(progress * HIT_FLASH_COUNT * 2);
        return phase % 2 == 0 ? 0.30 : 1.0;
    }

    private void startAnimationsForNewTurns(Battle battle, Map<String, List<Rectangle2D>> battlerAreasByName) {
        List<BattleTurn> turns = battle.getTurnLog();
        if (animatedTurnCount > turns.size()) {
            animatedTurnCount = 0;
        }
        long now = System.nanoTime();
        Set<String> animatedAttackKeys = new HashSet<>();
        for (int i = animatedTurnCount; i < turns.size(); i++) {
            BattleTurn turn = turns.get(i);
            if (turn.getAmount() <= 0 || turn.getTargetName().isBlank()) {
                continue;
            }
            hitFlashStartsByName.put(turn.getTargetName(), now);
            String spritePath = ATTACK_EFFECT_SPRITES.get(turn.getActionName());
            if (spritePath == null) {
                continue;
            }
            Rectangle2D targetArea = firstAreaFor(turn.getTargetName(), battlerAreasByName);
            if (targetArea == null) {
                continue;
            }
            String attackKey = turn.getTurnNumber() + "|" + turn.getActorName() + "|" + turn.getActionName();
            if (!animatedAttackKeys.add(attackKey)) {
                continue;
            }
            Rectangle2D actorArea = firstAreaFor(turn.getActorName(), battlerAreasByName);
            boolean projectile = isProjectileAttack(turn.getActionName()) && actorArea != null;
            double startX = projectile ? centerX(actorArea) : centerX(targetArea);
            double startY = projectile ? centerY(actorArea) : centerY(targetArea);
            double endX = centerX(targetArea);
            double endY = centerY(targetArea);
            effectAnimations.add(new EffectAnimation(spritePath, startX, startY, endX, endY, projectile, now));
        }
        animatedTurnCount = turns.size();
    }

    private Rectangle2D firstAreaFor(String battlerName, Map<String, List<Rectangle2D>> battlerAreasByName) {
        List<Rectangle2D> areas = battlerAreasByName.get(battlerName);
        return areas == null || areas.isEmpty() ? null : areas.getFirst();
    }

    private boolean isProjectileAttack(String attackName) {
        return switch (attackName) {
            case "Fireball", "Magic Missile", "Explosive Toss", "Pile Collapse" -> true;
            default -> false;
        };
    }

    private void drawEffectAnimations(GraphicsContext graphics) {
        long now = System.nanoTime();
        effectAnimations.removeIf(animation -> now - animation.startedAt() >= EFFECT_ANIMATION_NANOS);
        for (EffectAnimation animation : effectAnimations) {
            double progress = Math.min(1.0, (now - animation.startedAt()) / (double) EFFECT_ANIMATION_NANOS);
            double x = animation.projectile()
                    ? animation.startX() + (animation.endX() - animation.startX()) * progress
                    : animation.endX();
            double y = animation.projectile()
                    ? animation.startY() + (animation.endY() - animation.startY()) * progress
                    : animation.endY();
            double size = animation.projectile() ? PROJECTILE_EFFECT_SIZE : MELEE_EFFECT_SIZE;
            Image image = spriteLoader.load(animation.spritePath());
            if (image != null && !image.isError()) {
                graphics.drawImage(image, x - size / 2, y - size / 2, size, size);
            }
        }
    }

    private double centerX(Rectangle2D area) {
        return area.getMinX() + area.getWidth() / 2;
    }

    private double centerY(Rectangle2D area) {
        return area.getMinY() + area.getHeight() / 2;
    }

    private void drawHoveredTargetBox(GraphicsContext graphics, Map<Targetable, Rectangle2D> targetAreaByTarget,
                                      List<? extends Targetable> legalTargets, double hoverX, double hoverY) {
        if (Double.isNaN(hoverX) || Double.isNaN(hoverY)) {
            return;
        }
        for (Targetable target : legalTargets) {
            Rectangle2D area = targetAreaByTarget.get(target);
            if (area != null && area.contains(hoverX, hoverY)) {
                double blink = 0.45 + 0.40 * ((Math.sin(System.currentTimeMillis() / 220.0) + 1.0) / 2.0);
                graphics.setFill(Color.rgb(242, 238, 220, 0.12 + blink * 0.10));
                graphics.fillRect(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
                graphics.setStroke(Color.rgb(255, 255, 245, blink));
                graphics.setLineWidth(3);
                graphics.strokeRect(area.getMinX() - 3, area.getMinY() - 3,
                        area.getWidth() + 6, area.getHeight() + 6);
                return;
            }
        }
    }

    private SpriteLayout spriteLayout(Battler battler, double defaultSize) {
        return SPRITE_LAYOUTS.getOrDefault(normalizeName(battler.getName()),
                new SpriteLayout(0, 0, defaultSize, defaultSize));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase();
    }

    public boolean hasActiveDeathAnimations() {
        return !deathOffsets.isEmpty();
    }

    private boolean isDespawned(Battler battler) {
        return Boolean.TRUE.equals(despawnedBattlers.get(battler));
    }

    private double updateDeathOffset(Battler battler, double y, double height, double canvasHeight) {
        if (battler.isAlive()) {
            deathOffsets.remove(battler);
            despawnedBattlers.remove(battler);
            return 0;
        }
        double offset = deathOffsets.getOrDefault(battler, 0.0) + DEATH_FALL_SPEED;
        if (y + height + offset > canvasHeight + 12) {
            deathOffsets.remove(battler);
            despawnedBattlers.put(battler, true);
            return offset;
        }
        deathOffsets.put(battler, offset);
        return offset;
    }

    private record SpriteLayout(double xOffset, double yOffset, double width, double height) {
    }

    private record EnemyStackPosition(int columnIndex, int stackIndex, int nextVisibleIndex) {
    }

    private record EffectAnimation(String spritePath, double startX, double startY, double endX, double endY,
                                   boolean projectile, long startedAt) {
    }
}
