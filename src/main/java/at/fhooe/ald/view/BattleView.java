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
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class BattleView extends StackPane {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;
    private static final long TYPEWRITER_STEP_NANOS = 18_000_000L;
    private static final long MESSAGE_HOLD_NANOS = 1_000_000_000L;
    private static final long INTRO_HOLD_NANOS = 2_000_000_000L;
    private static final long TITLE_FADE_NANOS = 850_000_000L;
    private static final long RENDER_STEP_NANOS = 33_000_000L;

    private final GameController gameController;
    private final BattleService battleService;
    private final BattleRenderer battleRenderer;
    private final Runnable onVictory;
    private final Runnable onGameOver;
    private final Canvas canvas;
    private final List<Rectangle2D> attackAreas;
    private final List<Rectangle2D> targetAreas;
    private final Queue<String> pendingMessages;
    private final Queue<String> introMessages;
    private final AnimationTimer renderTimer;
    private Battle battle;
    private String statusMessage;
    private String fullStatusMessage;
    private BattleInputState inputState;
    private Attack selectedAttack;
    private List<Targetable> legalTargets;
    private boolean enemyActionsPending;
    private boolean logSequencePlaying;
    private boolean gameOverPending;
    private int visibleLogCharacters;
    private long lastTypewriterUpdate;
    private long messageCompletedAt;
    private double mouseX;
    private double mouseY;
    private boolean introSequenceActive;
    private IntroPhase introPhase;
    private String fullTitleMessage;
    private long titleCompletedAt;
    private long titleFadeStartedAt;
    private double titleOverlayAlpha;
    private double titleTextAlpha;

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
        this.introMessages = new ArrayDeque<>();
        this.renderTimer = createRenderTimer();
        this.statusMessage = null;
        this.fullStatusMessage = "";
        this.inputState = BattleInputState.INTRO_SEQUENCE;
        this.legalTargets = List.of();
        this.gameOverPending = false;
        this.mouseX = Double.NaN;
        this.mouseY = Double.NaN;
        this.introSequenceActive = false;
        this.introPhase = IntroPhase.NONE;
        this.fullTitleMessage = "";
        this.titleOverlayAlpha = 0.0;
        this.titleTextAlpha = 0.0;

        getChildren().add(canvas);
        setStyle("-fx-background-color: #111318;");
        canvas.setOnMouseClicked(event -> handleClick(event.getX(), event.getY()));
        canvas.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            render();
        });
        canvas.setOnMouseExited(event -> {
            mouseX = Double.NaN;
            mouseY = Double.NaN;
            render();
        });
        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                advanceInputFlow();
            }
        });
        beginIntroSequence();
        render();
        renderTimer.start();
    }

    private void handleClick(double x, double y) {
        requestFocus();
        if (introSequenceActive) {
            advanceIntroInput();
            return;
        }
        if (inputState == BattleInputState.WAIT_FOR_CONFIRM
                || inputState == BattleInputState.ENEMY_TURN) {
            advanceInputFlow();
            return;
        }
        if (battle.isFinished()) {
            handleFinishedBattleClick();
            return;
        }
        if (inputState == BattleInputState.SELECT_TARGET) {
            if (trySelectAttack(x, y)) {
                return;
            }
            for (int i = 0; i < targetAreas.size() && i < legalTargets.size(); i++) {
                if (targetAreas.get(i).contains(x, y)) {
                    resolveSelectedAttack(legalTargets.get(i));
                    return;
                }
            }
            showInteractiveMessage("Select a character to use " + selectedAttack.getName() + ".");
            return;
        }
        if (inputState == BattleInputState.SELECT_ATTACK) {
            trySelectAttack(x, y);
        }
    }

    private boolean trySelectAttack(double x, double y) {
        PlayerCharacter activeCharacter = activeCharacter();
        List<Attack> attacks = activeCharacter.getAttacks();
        for (int i = 0; i < attackAreas.size() && i < attacks.size(); i++) {
            if (attackAreas.get(i).contains(x, y)) {
                selectAttack(activeCharacter, attacks.get(i));
                return true;
            }
        }
        return false;
    }

    private void selectAttack(PlayerCharacter activeCharacter, Attack attack) {
        if (!battleService.isReady(activeCharacter, attack)) {
            showInteractiveMessage(attack.getName() + " is on cooldown.");
            return;
        }
        selectedAttack = attack;
        legalTargets = new ArrayList<>(battleService.getLegalTargets(battle, attack, true));
        if (legalTargets.isEmpty()) {
            resolveSelectedAttack(null);
            return;
        }
        inputState = BattleInputState.SELECT_TARGET;
        showInteractiveMessage("Click a blinking sprite to use " + attack.getName() + ".");
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
            if (isTypewriterRunning()) {
                revealFullMessage();
                return;
            }
            continueMessageSequence();
        }
    }

    private void resolveSelectedAttack(Targetable selectedTarget) {
        try {
            inputState = BattleInputState.RESOLVE_ACTION;
            PlayerCharacter activeCharacter = activeCharacter();
            int logStart = battle.getTurnLog().size();
            BattleResult result = battleService.usePlayerAttack(battle, activeCharacter, selectedAttack, selectedTarget);
            queueNewMessages(logStart);
            enemyActionsPending = result == BattleResult.IN_PROGRESS && isCurrentActorEnemy();
            inputState = BattleInputState.WAIT_FOR_CONFIRM;
            playNextQueuedMessage();
        } catch (RuntimeException exception) {
            showInteractiveMessage(exception.getMessage());
            resetActionSelection();
        }
        render();
    }

    private void performEnemyActions() {
        if (battle.getResult() != BattleResult.IN_PROGRESS) {
            return;
        }
        Battler currentActor = battle.getCurrentActor().orElse(null);
        if (currentActor instanceof Enemy enemy) {
            int logStart = battle.getTurnLog().size();
            battleService.performEnemyTurn(battle, enemy);
            queueNewMessages(logStart);
        }
        if (battle.getResult() == BattleResult.DEFEAT) {
            gameController.gameOver();
            return;
        }
        enemyActionsPending = battle.getResult() == BattleResult.IN_PROGRESS && isCurrentActorEnemy();
    }

    private void handleFinishedBattleClick() {
        if (battle.getResult() != BattleResult.VICTORY || gameController.getStatus() == GameStatus.VICTORY) {
            if (gameController.getStatus() == GameStatus.VICTORY) {
                renderTimer.stop();
                onVictory.run();
            }
            return;
        }
        try {
            battle = gameController.advanceAfterVictory();
            resetActionSelection();
            if (gameController.getStatus() == GameStatus.VICTORY) {
                renderTimer.stop();
                onVictory.run();
                return;
            }
            showInteractiveMessage("");
            beginIntroSequence();
        } catch (SQLException exception) {
            showInteractiveMessage("Could not load next floor: " + exception.getMessage());
        }
        render();
    }

    private PlayerCharacter activeCharacter() {
        Battler currentActor = battle.getCurrentActor()
                .orElseThrow(() -> new IllegalStateException("No actor can act"));
        if (currentActor instanceof PlayerCharacter playerCharacter) {
            return playerCharacter;
        }
        throw new IllegalStateException("It is " + currentActor.getName() + "'s turn.");
    }

    private void finishActionSequence() {
        if (battle.getResult() == BattleResult.VICTORY) {
            statusMessage = gameController.getStatus() == GameStatus.VICTORY
                    ? "Victory. You cleared all six floors."
                    : "Floor cleared. Click anywhere to continue.";
            inputState = BattleInputState.SELECT_ATTACK;
        } else if (battle.getResult() == BattleResult.DEFEAT) {
            gameController.gameOver();
            showInteractiveMessage("Game Over. Return to the main menu later.");
            inputState = BattleInputState.SELECT_ATTACK;
            selectedAttack = null;
            legalTargets = List.of();
            gameOverPending = true;
            return;
        } else {
            resetActionSelection();
            showInteractiveMessage("Choose an attack.");
        }
        render();
    }

    private void resetActionSelection() {
        inputState = BattleInputState.SELECT_ATTACK;
        selectedAttack = null;
        legalTargets = List.of();
        pendingMessages.clear();
        enemyActionsPending = false;
        logSequencePlaying = false;
    }

    private void beginIntroSequence() {
        resetActionSelection();
        inputState = BattleInputState.INTRO_SEQUENCE;
        introSequenceActive = true;
        introPhase = IntroPhase.TITLE_FADE_IN;
        introMessages.clear();
        battle.getIntroDialogue().stream()
                .sorted(Comparator.comparingInt(line -> line.getDisplayOrder()))
                .map(line -> line.getSpeaker().isBlank() ? line.getText() : line.getSpeaker() + ": " + line.getText())
                .forEach(introMessages::add);
        fullTitleMessage = "Floor " + battle.getFloorNumber() + ": " + battle.getFloorName();
        titleCompletedAt = 0;
        titleFadeStartedAt = System.nanoTime();
        titleOverlayAlpha = 1.0;
        titleTextAlpha = 0.0;
        fullStatusMessage = "";
        statusMessage = null;
        visibleLogCharacters = 0;
        messageCompletedAt = 0;
    }

    private void advanceIntroInput() {
        if (introPhase == IntroPhase.TITLE_FADE_IN) {
            titleOverlayAlpha = 1.0;
            titleTextAlpha = 1.0;
            titleCompletedAt = System.nanoTime();
            introPhase = IntroPhase.TITLE_HOLD;
            render();
            return;
        }
        if (introPhase == IntroPhase.DIALOGUE && isTypewriterRunning()) {
            revealFullMessage();
        }
    }

    private void queueNewMessages(int logStart) {
        battle.getTurnLog().stream()
                .skip(logStart)
                .map(turn -> turn.getMessage())
                .filter(message -> !message.isBlank())
                .filter(message -> !message.contains(" uses "))
                .forEach(pendingMessages::add);
    }

    private boolean showNextPendingMessage() {
        String nextMessage = pendingMessages.poll();
        if (nextMessage == null) {
            return false;
        }
        startTypewriterMessage(nextMessage, true);
        return true;
    }

    private void continueMessageSequence() {
        if (showNextPendingMessage()) {
            return;
        }
        if (enemyActionsPending) {
            enemyActionsPending = false;
            inputState = BattleInputState.ENEMY_TURN;
            performEnemyActions();
            if (showNextPendingMessage()) {
                inputState = BattleInputState.WAIT_FOR_CONFIRM;
                return;
            }
            if (enemyActionsPending) {
                continueMessageSequence();
                return;
            }
        }
        logSequencePlaying = false;
        finishActionSequence();
    }

    private void startAutomaticTurnsIfNeeded() {
        if (battle.getResult() == BattleResult.IN_PROGRESS && isCurrentActorEnemy()) {
            enemyActionsPending = true;
            inputState = BattleInputState.ENEMY_TURN;
            continueMessageSequence();
        }
    }

    private boolean isCurrentActorEnemy() {
        return battle.getCurrentActor().filter(Enemy.class::isInstance).isPresent();
    }

    private void playNextQueuedMessage() {
        if (!showNextPendingMessage()) {
            continueMessageSequence();
        }
    }

    private void showInteractiveMessage(String message) {
        startTypewriterMessage(message, false);
    }

    private void startTypewriterMessage(String message, boolean sequenceMessage) {
        fullStatusMessage = message == null ? "" : message;
        statusMessage = fullStatusMessage.isBlank() ? null : "";
        visibleLogCharacters = 0;
        lastTypewriterUpdate = 0;
        messageCompletedAt = 0;
        logSequencePlaying = sequenceMessage;
    }

    private boolean isTypewriterRunning() {
        return visibleLogCharacters < fullStatusMessage.length();
    }

    private void revealFullMessage() {
        visibleLogCharacters = fullStatusMessage.length();
        statusMessage = fullStatusMessage;
        messageCompletedAt = System.nanoTime();
        render();
    }

    private void updateTypewriter(long now) {
        if (visibleLogCharacters < fullStatusMessage.length()) {
            if (lastTypewriterUpdate == 0 || now - lastTypewriterUpdate >= TYPEWRITER_STEP_NANOS) {
                visibleLogCharacters++;
                statusMessage = fullStatusMessage.substring(0, visibleLogCharacters);
                lastTypewriterUpdate = now;
                if (visibleLogCharacters == fullStatusMessage.length()) {
                    messageCompletedAt = now;
                }
            }
            return;
        }
        if (logSequencePlaying && messageCompletedAt > 0 && now - messageCompletedAt >= MESSAGE_HOLD_NANOS) {
            continueMessageSequence();
        }
    }

    private void updateIntro(long now) {
        if (!introSequenceActive) {
            return;
        }
        if (introPhase == IntroPhase.TITLE_FADE_IN) {
            long elapsed = now - titleFadeStartedAt;
            titleOverlayAlpha = 1.0;
            titleTextAlpha = Math.min(1.0, elapsed / (double) TITLE_FADE_NANOS);
            if (elapsed >= TITLE_FADE_NANOS) {
                titleOverlayAlpha = 1.0;
                titleTextAlpha = 1.0;
                titleCompletedAt = now;
                introPhase = IntroPhase.TITLE_HOLD;
            }
            return;
        }
        if (introPhase == IntroPhase.TITLE_HOLD && titleCompletedAt > 0
                && now - titleCompletedAt >= INTRO_HOLD_NANOS) {
            introPhase = IntroPhase.TITLE_FADE_OUT;
            titleFadeStartedAt = now;
            return;
        }
        if (introPhase == IntroPhase.TITLE_FADE_OUT) {
            long elapsed = now - titleFadeStartedAt;
            titleOverlayAlpha = Math.max(0.0, 1.0 - elapsed / (double) TITLE_FADE_NANOS);
            titleTextAlpha = titleOverlayAlpha;
            if (elapsed >= TITLE_FADE_NANOS) {
                titleOverlayAlpha = 0.0;
                titleTextAlpha = 0.0;
                playNextIntroMessageOrFinish();
            }
            return;
        }
        if (introPhase == IntroPhase.DIALOGUE
                && !isTypewriterRunning()
                && messageCompletedAt > 0
                && now - messageCompletedAt >= INTRO_HOLD_NANOS) {
            playNextIntroMessageOrFinish();
        }
    }

    private void playNextIntroMessageOrFinish() {
        String introMessage = introMessages.poll();
        if (introMessage != null) {
            introPhase = IntroPhase.DIALOGUE;
            startTypewriterMessage(introMessage, false);
            return;
        }
        finishIntroSequence();
    }

    private void finishIntroSequence() {
        introSequenceActive = false;
        introPhase = IntroPhase.NONE;
        resetActionSelection();
        showInteractiveMessage("Choose an attack.");
        startAutomaticTurnsIfNeeded();
    }

    private void render() {
        attackAreas.clear();
        targetAreas.clear();
        if (battle.getAlivePartyMembers().isEmpty()) {
            fullStatusMessage = "Game Over. Return to the main menu later.";
            statusMessage = fullStatusMessage;
            visibleLogCharacters = fullStatusMessage.length();
        }
        BattleRenderResult renderResult = battleRenderer.render(
                canvas.getGraphicsContext2D(),
                battle,
                activeCharacterForRender(),
                selectedAttack,
                inputState == BattleInputState.SELECT_TARGET ? legalTargets : List.of(),
                battleService,
                statusMessage,
                canUseAttackButtons(),
                mouseX,
                mouseY,
                canvas.getWidth(),
                canvas.getHeight()
        );
        attackAreas.addAll(renderResult.getAttackAreas());
        targetAreas.addAll(renderResult.getTargetAreas());
        drawTitleOverlay();
    }

    private void drawTitleOverlay() {
        if (!introSequenceActive
                || (introPhase != IntroPhase.TITLE_FADE_IN
                && introPhase != IntroPhase.TITLE_HOLD
                && introPhase != IntroPhase.TITLE_FADE_OUT)) {
            return;
        }
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.save();
        graphics.setGlobalAlpha(titleOverlayAlpha);
        graphics.setFill(Color.BLACK);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.restore();

        graphics.save();
        graphics.setGlobalAlpha(titleTextAlpha);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.CENTER);
        graphics.setFont(Font.font("Consolas", 34));
        graphics.setFill(Color.rgb(242, 238, 220));
        graphics.fillText(fullTitleMessage, canvas.getWidth() / 2, canvas.getHeight() / 2);
        graphics.restore();
    }

    private PlayerCharacter activeCharacterForRender() {
        Battler currentActor = battle.getCurrentActor().orElse(null);
        if (currentActor instanceof PlayerCharacter playerCharacter) {
            return playerCharacter;
        }
        return battle.getAlivePartyMembers().isEmpty()
                ? battle.getParty().getFirst()
                : battle.getAlivePartyMembers().getFirst();
    }

    private boolean canUseAttackButtons() {
        return !battle.isFinished()
                && !introSequenceActive
                && battle.getCurrentActor().filter(PlayerCharacter.class::isInstance).isPresent()
                && (inputState == BattleInputState.SELECT_ATTACK || inputState == BattleInputState.SELECT_TARGET);
    }

    private AnimationTimer createRenderTimer() {
        return new AnimationTimer() {
            private long lastRender;

            @Override
            public void handle(long now) {
                updateTypewriter(now);
                updateIntro(now);
                if (now - lastRender >= RENDER_STEP_NANOS) {
                    render();
                    lastRender = now;
                }
                if (gameOverPending
                        && !battleRenderer.hasActiveDeathAnimations()
                        && !isTypewriterRunning()
                        && messageCompletedAt > 0
                        && now - messageCompletedAt >= MESSAGE_HOLD_NANOS) {
                    gameOverPending = false;
                    renderTimer.stop();
                    onGameOver.run();
                }
            }
        };
    }

    private enum IntroPhase {
        NONE,
        TITLE_FADE_IN,
        TITLE_HOLD,
        TITLE_FADE_OUT,
        DIALOGUE
    }
}
