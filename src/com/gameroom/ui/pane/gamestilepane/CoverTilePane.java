package com.gameroom.ui.pane.gamestilepane;

import com.gameroom.data.game.entry.GameEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.control.button.gamebutton.TileGameButton;
import com.gameroom.ui.scene.MainScene;

import static com.gameroom.ui.Main.SCREEN_HEIGHT;
import static com.gameroom.ui.Main.SCREEN_WIDTH;
import static com.gameroom.ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 14/08/2016.
 */
public class CoverTilePane extends GamesTilePane{
    public CoverTilePane(MainScene parentScene, String title) {
        super(parentScene);
        tilePane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));
        tilePane.setHgap(50 * SCREEN_WIDTH / 1920);
        tilePane.setVgap(70 * SCREEN_HEIGHT / 1080);
        setTitle(title);
        setPrefTileWidth(SCREEN_WIDTH / 4);
        setPrefTileHeight(tilePane.getPrefTileWidth() * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    public TilePane getTilePane() {
        return tilePane;
    }

    @Override
    public boolean isValidToAdd(GameEntry entry) {
        return !entry.isToAdd() && !entry.isDeleted() && ! entry.isIgnored();
    }

    @Override
    protected GameButton createGameButton(GameEntry newEntry) {
        return new TileGameButton(newEntry, tilePane, parentScene);
    }

    @Override
    public void sortByReleaseDate() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByReleaseDate(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).hideRating();
                    ((GameButton)button).showReleaseDate();
                }
            }
        });
    }

    @Override
    public void sortByRating() {
        Main.LOGGER.debug("Sorting by rating : "+titleLabel.getText());
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByRating(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).showRating();
                    ((GameButton)button).hideReleaseDate();
                }
            }
        });
    }

    @Override
    public void sortByTimePlayed() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByTimePlayed(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).showPlaytime();
                    ((GameButton)button).hideRating();
                    ((GameButton)button).hideReleaseDate();
                }
            }
        });
    }

    @Override
    public void sortByName() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByName(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).hideReleaseDate();
                    ((GameButton)button).hideRating();
                }
            }
        });
    }
}
