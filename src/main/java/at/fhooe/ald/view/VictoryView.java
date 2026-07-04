package at.fhooe.ald.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class VictoryView extends VBox {

    public VictoryView(int floorsCleared, int turnsTaken, Runnable onReturnToMenu) {
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setStyle("-fx-background-color: #20242a;");

        Label title = new Label("Victory");
        title.setFont(Font.font("Consolas", 36));
        title.setStyle("-fx-text-fill: #f2eedc;");

        Label summary = new Label("Floors cleared: " + floorsCleared + "   Turns: " + turnsTaken);
        summary.setFont(Font.font("Consolas", 18));
        summary.setStyle("-fx-text-fill: #d8c894;");

        Button menuButton = new Button("Return to Main Menu");
        menuButton.setFont(Font.font("Consolas", 16));
        menuButton.setMinWidth(220);
        menuButton.setOnAction(event -> onReturnToMenu.run());

        getChildren().addAll(title, summary, menuButton);
    }
}
