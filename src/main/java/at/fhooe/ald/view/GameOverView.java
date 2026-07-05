package at.fhooe.ald.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class GameOverView extends VBox {

    public GameOverView(int floorsCleared, int turnsTaken, Runnable onReturnToMenu) {
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setStyle("""
                -fx-background-color: #20242a;
                -fx-background-image: url('/assets/backgrounds/menus/GameOver_BG.png');
                -fx-background-size: cover;
                -fx-background-position: center;
                """);

        Label title = new Label("Game Over");
        title.setFont(Font.font("Consolas", 36));
        title.setStyle("""
                -fx-text-fill: #f2eedc;
                -fx-effect: dropshadow(gaussian, #000000, 8, 0.7, 0, 2);
                """);

        Label summary = new Label("Floors cleared: " + floorsCleared + "   Turns: " + turnsTaken);
        summary.setFont(Font.font("Consolas", 18));
        summary.setStyle("""
                -fx-text-fill: #d8c894;
                -fx-effect: dropshadow(gaussian, #000000, 8, 0.7, 0, 2);
                """);

        Button menuButton = new Button("Return to Main Menu");
        menuButton.setFont(Font.font("Consolas", 16));
        menuButton.setMinWidth(220);
        menuButton.setStyle("""
                -fx-background-color: rgba(18, 20, 24, 0.86);
                -fx-border-color: #ded1a5;
                -fx-text-fill: #f2eedc;
                -fx-padding: 10 28 10 28;
                """);
        menuButton.setOnAction(event -> onReturnToMenu.run());

        getChildren().addAll(title, summary, menuButton);
    }
}
