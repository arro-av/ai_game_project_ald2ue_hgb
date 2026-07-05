package at.fhooe.ald.view;

import at.fhooe.ald.service.audio.AudioCue;
import at.fhooe.ald.service.audio.AudioManager;
import at.fhooe.ald.service.audio.GameSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainMenuView extends StackPane {
    private static final String START_BUTTON_PATH = "/assets/ui/buttons/start_game.png";
    private static final String SETTINGS_BUTTON_PATH = "/assets/ui/buttons/settings.png";
    private static final String BACK_BUTTON_PATH = "/assets/ui/buttons/back.png";

    public MainMenuView(Runnable onStartGame, GameSettings settings, AudioManager audioManager) {
        setStyle("""
                -fx-background-color: #20242a;
                -fx-background-image: url('/assets/backgrounds/menus/Main_BG.png');
                -fx-background-size: cover;
                -fx-background-position: center;
                """);

        Button startButton = createImageButton(START_BUTTON_PATH, 390);
        startButton.setOnAction(event -> {
            audioManager.playSfx(AudioCue.UI_CLICK);
            onStartGame.run();
        });

        Button settingsButton = createImageButton(SETTINGS_BUTTON_PATH, 280);
        Button backButton = createImageButton(BACK_BUTTON_PATH, 260);
        VBox menuBox = new VBox(2, startButton, settingsButton);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setTranslateY(120);
        VBox settingsBox = createSettingsBox(settings, audioManager, backButton);
        settingsButton.setOnAction(event -> {
            audioManager.playSfx(AudioCue.UI_CLICK);
            menuBox.getChildren().setAll(settingsBox);
        });
        backButton.setOnAction(event -> {
            audioManager.playSfx(AudioCue.UI_CLICK);
            menuBox.getChildren().setAll(startButton, settingsButton);
        });

        Label disclaimer = new Label("""
                INFO: This is a fan project made only as a uni assignment → not in any way affiliated with Dungeon Crawler Carl. PLS don't sue me, I'm a HUGE fan! ♡ \
                """);
        disclaimer.setWrapText(true);
        disclaimer.setPrefWidth(1040);
        disclaimer.setMaxWidth(1040);
        disclaimer.setMinHeight(Region.USE_PREF_SIZE);
        disclaimer.setTextOverrun(OverrunStyle.CLIP);
        disclaimer.setAlignment(Pos.CENTER);
        disclaimer.setStyle("""
                -fx-background-color: rgba(10, 12, 16, 0.66);
                -fx-border-color: rgba(238, 226, 178, 0.32);
                -fx-padding: 8 14 8 14;
                -fx-font-family: Consolas;
                -fx-font-size: 12px;
                -fx-text-fill: #f2eedc;
                """);
        StackPane.setAlignment(disclaimer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(disclaimer, new Insets(0, 24, 18, 24));

        getChildren().addAll(menuBox, disclaimer);
    }

    private VBox createSettingsBox(GameSettings settings, AudioManager audioManager, Button backButton) {
        Label title = new Label("Settings");
        Slider musicSlider = createVolumeSlider(settings.getMusicVolume());
        Label musicLabel = new Label();
        updateVolumeLabel(musicLabel, "Music", musicSlider.getValue());
        musicSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.musicVolumeProperty().set(newValue.doubleValue());
            updateVolumeLabel(musicLabel, "Music", newValue.doubleValue());
        });

        Slider sfxSlider = createVolumeSlider(settings.getSfxVolume());
        Label sfxLabel = new Label();
        updateVolumeLabel(sfxLabel, "SFX", sfxSlider.getValue());
        sfxSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.sfxVolumeProperty().set(newValue.doubleValue());
            updateVolumeLabel(sfxLabel, "SFX", newValue.doubleValue());
        });
        sfxSlider.setOnMouseReleased(event -> audioManager.playSfx(AudioCue.UI_CLICK));

        VBox box = new VBox(7,
                title,
                musicLabel,
                musicSlider,
                sfxLabel,
                sfxSlider,
                backButton
        );
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(300);
        box.setStyle("""
                -fx-background-color: rgba(10, 12, 16, 0.58);
                -fx-border-color: rgba(238, 226, 178, 0.34);
                -fx-padding: 10 16 12 16;
                """);
        title.setStyle(settingsTitleStyle());
        musicLabel.setStyle(settingsLabelStyle());
        sfxLabel.setStyle(settingsLabelStyle());
        return box;
    }

    private Button createImageButton(String resourcePath, double fitWidth) {
        ImageView imageView = new ImageView(new Image(
                MainMenuView.class.getResourceAsStream(resourcePath)
        ));
        imageView.setFitWidth(fitWidth);
        imageView.setPreserveRatio(true);

        Button button = new Button();
        button.setGraphic(imageView);
        button.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-padding: 0;
                """);
        button.setOnMouseEntered(event -> {
            button.setScaleX(1.06);
            button.setScaleY(1.06);
        });
        button.setOnMouseExited(event -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        return button;
    }

    private Slider createVolumeSlider(double value) {
        Slider slider = new Slider(0.0, 1.0, value);
        slider.setMaxWidth(250);
        slider.setBlockIncrement(0.05);
        return slider;
    }

    private void updateVolumeLabel(Label label, String name, double value) {
        label.setText(name + ": " + Math.round(value * 100) + "%");
    }

    private String settingsLabelStyle() {
        return """
                -fx-font-family: Consolas;
                -fx-font-size: 12px;
                -fx-text-fill: #f2eedc;
                """;
    }

    private String settingsTitleStyle() {
        return """
                -fx-font-family: Consolas;
                -fx-font-size: 15px;
                -fx-text-fill: #f2eedc;
                """;
    }
}
