package at.fhooe.ald.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class MainMenuView extends VBox {

    public MainMenuView(Runnable onStartGame) {
        setAlignment(Pos.CENTER);
        setSpacing(22);
        setStyle("""
                -fx-background-color: #20242a;
                -fx-background-image: url('/assets/backgrounds/menus/Main_BG.png');
                -fx-background-size: cover;
                -fx-background-position: center;
                """);

        ImageView startImage = new ImageView(new Image(
                MainMenuView.class.getResourceAsStream("/assets/ui/buttons/start_game.png")
        ));
        startImage.setFitWidth(390);
        startImage.setPreserveRatio(true);

        Button startButton = new Button();
        startButton.setGraphic(startImage);
        startButton.setTranslateY(140);
        startButton.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-padding: 0;
                -fx-cursor: hand;
                """);
        startButton.setOnMouseEntered(event -> {
            startButton.setScaleX(1.06);
            startButton.setScaleY(1.06);
        });
        startButton.setOnMouseExited(event -> {
            startButton.setScaleX(1.0);
            startButton.setScaleY(1.0);
        });
        startButton.setOnAction(event -> onStartGame.run());

        getChildren().add(startButton);
    }
}
