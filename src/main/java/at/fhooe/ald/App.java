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
import at.fhooe.ald.service.audio.AudioManager;
import at.fhooe.ald.service.audio.GameSettings;
import at.fhooe.ald.view.BattleView;
import at.fhooe.ald.view.GameOverView;
import at.fhooe.ald.view.MainMenuView;
import at.fhooe.ald.view.VictoryView;
import at.fhooe.ald.view.render.BattleRenderer;
import at.fhooe.ald.view.render.HudRenderer;
import at.fhooe.ald.view.render.SpriteLoader;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {
    private static final double SCENE_WIDTH = 1280;
    private static final double SCENE_HEIGHT = 720;
    private static final String CURSOR_PATH = "/assets/ui/icons/cursor.png";
    private static final double CURSOR_SCALE = 0.08;

    private Stage stage;
    private GameController gameController;
    private BattleService battleService;
    private BattleRenderer battleRenderer;
    private Image gameCursorImage;
    private GameSettings gameSettings;
    private AudioManager audioManager;

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
        gameSettings = new GameSettings();
        audioManager = new AudioManager(gameSettings);
    }

    private void showMainMenu() {
        audioManager.playMainMenuMusic();
        stage.setScene(createScene(new MainMenuView(this::startGame, gameSettings, audioManager)));
    }

    private void startGame() {
        try {
            Battle battle = gameController.startGame();
            stage.setScene(createScene(new BattleView(
                    gameController,
                    battleService,
                    battleRenderer,
                    battle,
                    this::showVictory,
                    this::showGameOver,
                    audioManager
            )));
        } catch (Exception exception) {
            Label error = new Label("Could not start game: " + exception.getMessage());
            StackPane root = new StackPane(error);
            root.setStyle("-fx-background-color: #20242a; -fx-text-fill: #f2eedc;");
            stage.setScene(createScene(root));
        }
    }

    private void showVictory() {
        saveRunResult();
        stage.setScene(createScene(new VictoryView(
                gameController.getFloorsCleared(),
                gameController.getTurnsTaken(),
                this::showMainMenu
        )));
    }

    private void showGameOver() {
        saveRunResult();
        stage.setScene(createScene(new GameOverView(
                gameController.getFloorsCleared(),
                gameController.getTurnsTaken(),
                this::showMainMenu
        )));
    }

    private Scene createScene(Parent root) {
        Image cursorImage = getGameCursorImage();
        if (cursorImage == null) {
            return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        }
        ImageView cursorView = new ImageView(cursorImage);
        cursorView.setFitWidth(cursorImage.getWidth() * CURSOR_SCALE);
        cursorView.setFitHeight(cursorImage.getHeight() * CURSOR_SCALE);
        cursorView.setMouseTransparent(true);
        cursorView.setManaged(false);
        cursorView.setSmooth(false);
        cursorView.setVisible(false);

        Pane cursorLayer = new Pane(cursorView);
        cursorLayer.setMouseTransparent(true);
        StackPane sceneRoot = new StackPane(root, cursorLayer);
        Scene scene = new Scene(sceneRoot, SCENE_WIDTH, SCENE_HEIGHT);
        scene.setCursor(Cursor.NONE);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> moveCursor(cursorView, event));
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> moveCursor(cursorView, event));
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> moveCursor(cursorView, event));
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
            cursorView.setVisible(true);
            moveCursor(cursorView, event);
        });
        scene.addEventFilter(MouseEvent.MOUSE_EXITED, event -> cursorView.setVisible(false));
        return scene;
    }

    private void moveCursor(ImageView cursorView, MouseEvent event) {
        cursorView.relocate(event.getSceneX(), event.getSceneY());
        cursorView.setVisible(true);
    }

    private Image getGameCursorImage() {
        if (gameCursorImage != null) {
            return gameCursorImage;
        }
        var resource = App.class.getResource(CURSOR_PATH);
        if (resource == null) {
            return null;
        }
        Image original = new Image(resource.toExternalForm());
        if (original.isError()) {
            return null;
        }
        gameCursorImage = original;
        return gameCursorImage;
    }

    private void saveRunResult() {
        try {
            gameController.saveRunResultIfNeeded();
        } catch (Exception exception) {
            System.err.println("Could not save run result: " + exception.getMessage());
        }
    }
}
