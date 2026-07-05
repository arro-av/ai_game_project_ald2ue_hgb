package at.fhooe.ald;

import at.fhooe.ald.controller.GameController;
import at.fhooe.ald.dao.jdbc.DatabaseInitializer;
import at.fhooe.ald.dao.jdbc.JdbcAttackDao;
import at.fhooe.ald.dao.jdbc.JdbcCharacterDao;
import at.fhooe.ald.dao.jdbc.JdbcDialogueDao;
import at.fhooe.ald.dao.jdbc.JdbcEnemyDao;
import at.fhooe.ald.dao.jdbc.JdbcFloorDao;
import at.fhooe.ald.dao.jdbc.JdbcHighScoreDao;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.service.BattleService;
import at.fhooe.ald.service.DamageCalculator;
import at.fhooe.ald.service.DialogueService;
import at.fhooe.ald.service.EncounterService;
import at.fhooe.ald.service.GameService;
import at.fhooe.ald.service.HighScoreService;
import at.fhooe.ald.service.TargetSelector;
import at.fhooe.ald.view.BattleView;
import at.fhooe.ald.view.GameOverView;
import at.fhooe.ald.view.MainMenuView;
import at.fhooe.ald.view.VictoryView;
import at.fhooe.ald.view.render.BattleRenderer;
import at.fhooe.ald.view.render.HudRenderer;
import at.fhooe.ald.view.render.SpriteLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {
    private static final double SCENE_WIDTH = 1280;
    private static final double SCENE_HEIGHT = 720;

    private Stage stage;
    private GameController gameController;
    private BattleService battleService;
    private BattleRenderer battleRenderer;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        configureServices();
        stage.setTitle("Dungeon Crawler Carl");
        showMainMenu();
        stage.show();
    }

    private void configureServices() throws Exception {
        var database = new DatabaseInitializer().initialize();
        var attackDao = new JdbcAttackDao(database);
        var characterDao = new JdbcCharacterDao(database, attackDao);
        var enemyDao = new JdbcEnemyDao(database, attackDao);
        var floorDao = new JdbcFloorDao(database, enemyDao);
        var dialogueDao = new JdbcDialogueDao(database);
        var highScoreDao = new JdbcHighScoreDao(database);

        var dialogueService = new DialogueService(dialogueDao);
        var encounterService = new EncounterService(floorDao, characterDao, dialogueService);
        var gameService = new GameService(encounterService, floorDao);
        var highScoreService = new HighScoreService(highScoreDao);

        gameController = new GameController(gameService, highScoreService);
        battleService = new BattleService(new DamageCalculator(), new TargetSelector());
        battleRenderer = new BattleRenderer(new SpriteLoader(), new HudRenderer());
    }

    private void showMainMenu() {
        stage.setScene(new Scene(new MainMenuView(this::startGame), SCENE_WIDTH, SCENE_HEIGHT));
    }

    private void startGame() {
        try {
            Battle battle = gameController.startGame();
            stage.setScene(new Scene(new BattleView(
                    gameController,
                    battleService,
                    battleRenderer,
                    battle,
                    this::showVictory,
                    this::showGameOver
            ), SCENE_WIDTH, SCENE_HEIGHT));
        } catch (Exception exception) {
            Label error = new Label("Could not start game: " + exception.getMessage());
            StackPane root = new StackPane(error);
            root.setStyle("-fx-background-color: #20242a; -fx-text-fill: #f2eedc;");
            stage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));
        }
    }

    private void showVictory() {
        saveRunResult();
        stage.setScene(new Scene(new VictoryView(
                gameController.getFloorsCleared(),
                gameController.getTurnsTaken(),
                this::showMainMenu
        ), SCENE_WIDTH, SCENE_HEIGHT));
    }

    private void showGameOver() {
        saveRunResult();
        stage.setScene(new Scene(new GameOverView(
                gameController.getFloorsCleared(),
                gameController.getTurnsTaken(),
                this::showMainMenu
        ), SCENE_WIDTH, SCENE_HEIGHT));
    }

    private void saveRunResult() {
        try {
            gameController.saveRunResultIfNeeded();
        } catch (Exception exception) {
            System.err.println("Could not save run result: " + exception.getMessage());
        }
    }
}
