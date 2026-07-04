package at.fhooe.ald.view;

import at.fhooe.ald.controller.GameController;
import at.fhooe.ald.model.Attack;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.BattleResult;
import at.fhooe.ald.model.Battler;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.service.BattleService;
import at.fhooe.ald.service.GameStatus;
import at.fhooe.ald.view.render.BattleRenderResult;
import at.fhooe.ald.view.render.BattleRenderer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

public class BattleView extends StackPane {
    private static final double WIDTH = 960;
    private static final double HEIGHT = 540;

    private final GameController gameController;
    private final BattleService battleService;
    private final BattleRenderer battleRenderer;
    private final Runnable onVictory;
    private final Runnable onGameOver;
    private final Canvas canvas;
    private final List<Rectangle2D> attackAreas;
    private final List<Rectangle2D> targetAreas;
    private final Queue<String> pendingMessages;
    private Battle battle;
    private String statusMessage;
    private int activePartyIndex;
    private BattleInputState inputState;
    private Attack selectedAttack;
    private List<Targetable> legalTargets;
    private boolean enemyActionsPending;

    public BattleView(GameController gameController, BattleService battleService, BattleRenderer battleRenderer,
                      Battle battle, Runnable onVictory, Runnable onGameOver) {
        this.gameController = gameController;
        this.battleService = battleService;
        this.battleRenderer = battleRenderer;
        this.onVictory = onVictory;
        this.onGameOver = onGameOver;
        this.battle = battle;
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.attackAreas = new ArrayList<>();
        this.targetAreas = new ArrayList<>();
        this.pendingMessages = new ArrayDeque<>();
        this.statusMessage = "";
        this.activePartyIndex = 0;
        this.inputState = BattleInputState.SELECT_ATTACK;
        this.legalTargets = List.of();

        getChildren().add(canvas);
        setStyle("-fx-background-color: #111318;");
        canvas.setOnMouseClicked(event -> handleClick(event.getX(), event.getY()));
        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                advanceInputFlow();
            }
        });
        render();
    }

    private void handleClick(double x, double y) {
        requestFocus();
        if (inputState == BattleInputState.WAIT_FOR_CONFIRM
                || inputState == BattleInputState.SHOW_ATTACK_DESCRIPTION
                || inputState == BattleInputState.ENEMY_TURN) {
            advanceInputFlow();
            return;
        }
        if (battle.isFinished()) {
            handleFinishedBattleClick();
            return;
        }
        if (inputState == BattleInputState.SELECT_TARGET) {
            for (int i = 0; i < targetAreas.size() && i < legalTargets.size(); i++) {
                if (targetAreas.get(i).contains(x, y)) {
                    resolveSelectedAttack(legalTargets.get(i));
                    return;
                }
            }
            statusMessage = "Choose a highlighted target for " + selectedAttack.getName() + ".";
            render();
            return;
        }
        if (inputState == BattleInputState.SELECT_ATTACK) {
            PlayerCharacter activeCharacter = activeCharacter();
            List<Attack> attacks = activeCharacter.getAttacks();
            for (int i = 0; i < attackAreas.size() && i < attacks.size(); i++) {
                if (attackAreas.get(i).contains(x, y)) {
                    selectAttack(activeCharacter, attacks.get(i));
                    return;
                }
            }
        }
    }

    private void selectAttack(PlayerCharacter activeCharacter, Attack attack) {
        if (!battleService.isReady(activeCharacter, attack)) {
            statusMessage = attack.getName() + " is on cooldown.";
            render();
            return;
        }
        selectedAttack = attack;
        legalTargets = new ArrayList<>(battleService.getLegalTargets(battle, attack, true));
        inputState = BattleInputState.SHOW_ATTACK_DESCRIPTION;
        statusMessage = attack.getName() + ": " + attack.getDescription() + " Press Enter or click to continue.";
        render();
    }

    private void advanceInputFlow() {
        if (inputState == BattleInputState.SHOW_ATTACK_DESCRIPTION) {
            if (selectedAttack == null) {
                resetActionSelection();
                render();
                return;
            }
            if (!legalTargets.isEmpty()) {
                inputState = BattleInputState.SELECT_TARGET;
                statusMessage = "Choose a target for " + selectedAttack.getName() + ".";
                render();
            } else {
                resolveSelectedAttack(null);
            }
            return;
        }

        if (inputState == BattleInputState.WAIT_FOR_CONFIRM || inputState == BattleInputState.ENEMY_TURN) {
            if (showNextPendingMessage()) {
                render();
                return;
            }
            if (enemyActionsPending) {
                enemyActionsPending = false;
                inputState = BattleInputState.ENEMY_TURN;
                performEnemyActions();
                if (showNextPendingMessage()) {
                    inputState = BattleInputState.WAIT_FOR_CONFIRM;
                    render();
                    return;
                }
            }
            finishActionSequence();
        }
    }

    private void resolveSelectedAttack(Targetable selectedTarget) {
        try {
            inputState = BattleInputState.RESOLVE_ACTION;
            PlayerCharacter activeCharacter = activeCharacter();
            int logStart = battle.getTurnLog().size();
            BattleResult result = battleService.usePlayerAttack(battle, activeCharacter, selectedAttack, selectedTarget);
            queueNewMessages(logStart);
            if (result == BattleResult.IN_PROGRESS) {
                enemyActionsPending = true;
            } else {
                enemyActionsPending = false;
            }
            inputState = BattleInputState.WAIT_FOR_CONFIRM;
            showNextPendingMessage();
        } catch (RuntimeException exception) {
            statusMessage = exception.getMessage();
            resetActionSelection();
        }
        render();
    }

    private void performEnemyActions() {
        for (Enemy enemy : battle.getAliveEnemies()) {
            if (battle.getResult() != BattleResult.IN_PROGRESS) {
                return;
            }
            int logStart = battle.getTurnLog().size();
            battleService.performEnemyTurn(battle, enemy);
            queueNewMessages(logStart);
        }
        if (battle.getResult() == BattleResult.DEFEAT) {
            gameController.gameOver();
        }
    }

    private void handleFinishedBattleClick() {
        if (battle.getResult() != BattleResult.VICTORY || gameController.getStatus() == GameStatus.VICTORY) {
            if (gameController.getStatus() == GameStatus.VICTORY) {
                onVictory.run();
            }
            return;
        }
        try {
            battle = gameController.advanceAfterVictory();
            activePartyIndex = 0;
            resetActionSelection();
            if (gameController.getStatus() == GameStatus.VICTORY) {
                onVictory.run();
                return;
            }
            statusMessage = "";
        } catch (SQLException exception) {
            statusMessage = "Could not load next floor: " + exception.getMessage();
        }
        render();
    }

    private PlayerCharacter activeCharacter() {
        List<PlayerCharacter> aliveParty = battle.getAlivePartyMembers();
        if (aliveParty.isEmpty()) {
            throw new IllegalStateException("No party member can act");
        }
        activePartyIndex = Math.floorMod(activePartyIndex, aliveParty.size());
        return aliveParty.get(activePartyIndex);
    }

    private void advanceActiveCharacter() {
        int aliveCount = battle.getAlivePartyMembers().size();
        if (aliveCount > 0) {
            activePartyIndex = (activePartyIndex + 1) % aliveCount;
        }
    }

    private void finishActionSequence() {
        if (battle.getResult() == BattleResult.VICTORY) {
            statusMessage = gameController.getStatus() == GameStatus.VICTORY
                    ? "Victory. You cleared all six floors."
                    : "Floor cleared. Click anywhere to continue.";
            inputState = BattleInputState.SELECT_ATTACK;
        } else if (battle.getResult() == BattleResult.DEFEAT) {
            gameController.gameOver();
            statusMessage = "Game Over. Return to the main menu later.";
            inputState = BattleInputState.SELECT_ATTACK;
            onGameOver.run();
            return;
        } else {
            advanceActiveCharacter();
            resetActionSelection();
            statusMessage = "Choose an attack.";
        }
        render();
    }

    private void resetActionSelection() {
        inputState = BattleInputState.SELECT_ATTACK;
        selectedAttack = null;
        legalTargets = List.of();
        pendingMessages.clear();
        enemyActionsPending = false;
    }

    private void queueNewMessages(int logStart) {
        battle.getTurnLog().stream()
                .skip(logStart)
                .map(turn -> turn.getMessage())
                .filter(message -> !message.isBlank())
                .forEach(pendingMessages::add);
    }

    private boolean showNextPendingMessage() {
        String nextMessage = pendingMessages.poll();
        if (nextMessage == null) {
            return false;
        }
        statusMessage = nextMessage;
        return true;
    }

    private void render() {
        attackAreas.clear();
        targetAreas.clear();
        if (battle.getAlivePartyMembers().isEmpty()) {
            statusMessage = "Game Over. Return to the main menu later.";
        }
        List<Battler> turnPreview = battleService.getTurnPreview(battle, 5);
        BattleRenderResult renderResult = battleRenderer.render(
                canvas.getGraphicsContext2D(),
                battle,
                activeCharacterOrFallback(),
                selectedAttack,
                inputState == BattleInputState.SELECT_TARGET ? legalTargets : List.of(),
                turnPreview,
                battleService,
                statusMessage,
                canvas.getWidth(),
                canvas.getHeight()
        );
        attackAreas.addAll(renderResult.getAttackAreas());
        targetAreas.addAll(renderResult.getTargetAreas());
    }

    private PlayerCharacter activeCharacterOrFallback() {
        return battle.getAlivePartyMembers().stream().findFirst().orElse(battle.getParty().getFirst());
    }
}
