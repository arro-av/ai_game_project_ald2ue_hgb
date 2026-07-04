package at.fhooe.ald.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class MainMenuView extends VBox {

    public MainMenuView(Runnable onStartGame) {
        setAlignment(Pos.CENTER);
        setSpacing(22);
        setStyle("-fx-background-color: #20242a;");

        Label title = new Label("Dungeon Crawler Carl");
        title.setFont(Font.font("Consolas", 34));
        title.setStyle("-fx-text-fill: #f2eedc;");

        Button startButton = new Button("Start Game");
        startButton.setFont(Font.font("Consolas", 18));
        startButton.setMinWidth(180);
        startButton.setOnAction(event -> onStartGame.run());

        getChildren().addAll(title, startButton);
    }
}
