package ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import ui.Main;


/**
 * Created by LM on 11/08/2016.
 */
public class ConsoleOutputDialog extends GameRoomDialog {
    private Label textLabel = new Label("");
    private boolean showing = false;

    public ConsoleOutputDialog(){
        textLabel.setWrapText(true);
        textLabel.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        textLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                "    -fx-background-color: derive(-dark, 20%);\n"+
                "    -fx-font-size: 16.0px;\n" +
                "    -fx-font-weight: 600;");
        mainPane.setTop(new javafx.scene.control.Label("Console"));
        ScrollPane pane = new ScrollPane();
        pane.setFitToWidth(true);
        pane.setContent(textLabel);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(mainPane.getTop(), Pos.CENTER);
        BorderPane.setMargin(mainPane.getTop(),new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

        mainPane.setCenter(pane);


        getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE));
        setOnHiding(event -> {
            textLabel.setText("");
            showing = false;
        });
    }
    public void appendLine(String line){
        textLabel.setText(textLabel.getText()+"\n"+line);
    }
    public void showConsole(){
        if(!showing){
            showing = true;
            showAndWait();
        }
    }
}