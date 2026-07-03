package at.fhooe.ald;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Dungeon Crawler Carl");
        StackPane root = new StackPane(title);
        Scene scene = new Scene(root, 960, 540);

        stage.setTitle("Dungeon Crawler Carl");
        stage.setScene(scene);
        stage.show();
    }
}
